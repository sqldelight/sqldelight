/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.sqldelight.core.compiler

import app.cash.sqldelight.core.SqlDelightFileIndex
import app.cash.sqldelight.core.capitalize
import app.cash.sqldelight.core.compiler.model.NamedQuery
import app.cash.sqldelight.core.compiler.model.SelectQueryable
import app.cash.sqldelight.core.lang.MigrationFile
import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.core.lang.SqlDelightQueriesFile
import app.cash.sqldelight.core.lang.queriesName
import app.cash.sqldelight.core.lang.util.sqFile
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import com.alecstrong.sql.psi.core.psi.InvalidElementDetectedException
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.NamedElement
import com.alecstrong.sql.psi.core.psi.SqlCreateViewStmt
import com.intellij.configurationStore.serialize
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.PsiElement
import com.squareup.kotlinpoet.*
import java.io.Closeable
import java.io.File

private typealias FileAppender = (fileName: String) -> Appendable

object SqlDelightCompiler {
  fun writeInterfaces(
    module: Module,
    dialect: SqlDelightDialect,
    file: SqlDelightQueriesFile,
    output: FileAppender,
  ) {
    try {
      writeTableInterfaces(file, output)
      writeQueryInterfaces(file, output)
      writeQueries(module, dialect, file, output)
    } catch (e: InvalidElementDetectedException) {
      // It's okay if compilation is cut short, we can just quit out.
    }
  }

  fun writeInterfaces(
    file: MigrationFile,
    output: FileAppender,
    includeAll: Boolean = false,
  ) {
    writeTableInterfaces(file, output, includeAll)
  }

  fun writeDatabaseInterface(
    module: Module,
    file: SqlDelightFile,
    implementationFolder: String,
    output: FileAppender,
  ) {
    writeQueryWrapperInterface(module, file, implementationFolder, output)
  }

  fun writeImplementations(
    module: Module,
    sourceFile: SqlDelightFile,
    implementationFolder: String,
    output: FileAppender,
  ) {
    val fileIndex = SqlDelightFileIndex.getInstance(module)
    val packageName = "${fileIndex.packageName}.$implementationFolder"
    val databaseImplementationType = DatabaseGenerator(module, sourceFile).type()
    val exposer = DatabaseExposerGenerator(databaseImplementationType, fileIndex, sourceFile.generateAsync)

    val fileSpec = FileSpec.builder(packageName, databaseImplementationType.name!!)
      .addProperty(exposer.exposedSchema())
      .addFunction(exposer.exposedConstructor())
      .addType(databaseImplementationType)
      .build()

    fileIndex.outputDirectory(sourceFile).forEach { outputDirectory ->
      val packageDirectory = "$outputDirectory/${packageName.replace(".", "/")}"
      fileSpec.writeToAndClose(output("$packageDirectory/${databaseImplementationType.name}.kt"))
    }
  }

  private fun writeQueryWrapperInterface(
    module: Module,
    sourceFile: SqlDelightFile,
    implementationFolder: String,
    output: FileAppender,
  ) {
    val fileIndex = SqlDelightFileIndex.getInstance(module)
    val packageName = fileIndex.packageName
    val queryWrapperType = DatabaseGenerator(module, sourceFile).interfaceType()
    val fileSpec = FileSpec.builder(packageName, queryWrapperType.name!!)
      // TODO: Remove these when kotlinpoet supports top level types.
      .addImport("$packageName.$implementationFolder", "newInstance", "schema")
      .apply {
        var index = 0
        fileIndex.dependencies.forEach {
          addAliasedImport(ClassName(it.packageName, it.className), "${it.className}${index++}")
        }
      }
      .addType(queryWrapperType)
      .build()

    fileIndex.outputDirectory(sourceFile).forEach { outputDirectory ->
      val packageDirectory = "$outputDirectory/${packageName.replace(".", "/")}"
      fileSpec.writeToAndClose(output("$packageDirectory/${queryWrapperType.name}.kt"))
    }
  }

  internal fun writeTableInterfaces(
    file: SqlDelightFile,
    output: FileAppender,
    includeAll: Boolean = false,
  ) {
    val packageName = file.packageName ?: return
    file.tables(includeAll).forEach { query ->
      val statement = query.tableName.parent

      if (statement is SqlCreateViewStmt && statement.compoundSelectStmt != null) {
        listOf(NamedQuery(allocateName(statement.viewName), SelectQueryable(statement.compoundSelectStmt!!)))
          .writeQueryInterfaces(file, output)
        return@forEach
      }

      val typeSpec = tryWithElement(statement) {
        val generator = TableInterfaceGenerator(query)
        generator.kotlinImplementationSpec()
      }

      val fileSpec = FileSpec.builder(packageName, allocateName(query.tableName))
        .apply { if (typeSpec != null) addType(typeSpec) }
        .build()

      val serializerFileSpec = FileSpec.builder(packageName, "${query.tableName.name}Serializer")
        .apply {
          if (typeSpec != null) addType(
            SnapshotSerializerGenerator(
              packageName,
              typeSpec,
              query
            ).implementationSpec()
          )
        }
        .build()

      statement.sqFile().generatedDirectories?.forEach { directory ->
        fileSpec.writeToAndClose(output("$directory/${allocateName(query.tableName).capitalize()}.kt"))
      }

      statement.sqFile().testGeneratedDirectories?.forEach { directory ->
        serializerFileSpec.writeToAndClose(output("$directory/${allocateName(query.tableName).capitalize()}Serializer.kt"))
      }
    }
  }

  internal fun writeTestSerializersModule(
    module: Module,
    files: List<SqlDelightFile>,
  ) {
    val index = SqlDelightFileIndex.getInstance(module)

    val builder =
      PropertySpec.builder("${index.className}SerializersModule", ClassName("kotlinx.serialization.modules", "SerializersModule"))
    val initializer = CodeBlock.builder()
      .beginControlFlow("%M", MemberName("kotlinx.serialization.modules", "SerializersModule"))

    files.forEach { file ->
      val packageName = file.packageName ?: return@forEach

      file.tables(false).forEach { query ->
        val name = allocateName(query.tableName).capitalize()
        initializer.addStatement(
          "contextual(%T::class, %T)",
          ClassName(packageName, name),
          ClassName(packageName, "${name}Serializer")
        )
      }
    }

    val moduleSpec = builder.initializer(initializer.endControlFlow().build()).build()
    val fileSpec = FileSpec.builder(index.packageName, "${index.className}SerializersModule")
      .addProperty(moduleSpec)
      .build()

    index.testOutputDirectories().forEach { directory ->
      fileSpec.writeTo(File(directory))
    }
  }

  internal fun writeQueryInterfaces(
    file: SqlDelightQueriesFile,
    output: FileAppender,
  ) {
    file.namedQueries.writeQueryInterfaces(file, output)
  }

  internal fun writeQueries(
    module: Module,
    dialect: SqlDelightDialect,
    file: SqlDelightQueriesFile,
    output: FileAppender,
  ) {
    val packageName = file.packageName ?: return
    val queriesType = QueriesTypeGenerator(module, file, dialect)
      .generateType(packageName) ?: return

    val fileSpec = FileSpec.builder(packageName, file.queriesName.capitalize())
      .addType(queriesType)
      .build()

    file.generatedDirectories?.forEach { directory ->
      fileSpec.writeToAndClose(output("$directory/${queriesType.name}.kt"))
    }
  }

  internal fun allocateName(namedElement: NamedElement): String {
    return NameAllocator().newName(namedElement.normalizedName)
  }

  private fun List<NamedQuery>.writeQueryInterfaces(file: SqlDelightFile, output: FileAppender) {
    return filter { tryWithElement(it.select) { it.needsInterface() } == true }
      .forEach { namedQuery ->
        val fileSpec = FileSpec.builder(namedQuery.interfaceType.packageName, namedQuery.name)
          .apply {
            tryWithElement(namedQuery.select) {
              val generator = QueryInterfaceGenerator(namedQuery)
              addType(generator.kotlinImplementationSpec())
            }
          }
          .build()

        file.generatedDirectories(namedQuery.interfaceType.packageName)?.forEach { directory ->
          fileSpec.writeToAndClose(output("$directory/${namedQuery.name.capitalize()}.kt"))
        }
      }
  }

  private val NamedElement.normalizedName: String
    get() {
      val f = name[0]
      val l = name[name.lastIndex]
      return if ((f in "\"'`" && f == l) || (f == '[' && l == ']')) {
        name.substring(1, name.length - 1)
      } else {
        name
      }
    }

  private fun FileSpec.writeToAndClose(appendable: Appendable) {
    writeTo(appendable)
    if (appendable is Closeable) appendable.close()
  }
}

internal fun <T> tryWithElement(
  element: PsiElement,
  block: () -> T,
): T? {
  try {
    return block()
  } catch (e: ProcessCanceledException) {
    throw e
  } catch (e: InvalidElementDetectedException) {
    // It's okay if compilation is cut short, we can just quit out.
    return null
  } catch (e: Throwable) {
    val exception = IllegalStateException("Failed to compile $element :\n${element.text}")
    exception.initCause(e)
    throw exception
  }
}
