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
package com.squareup.sqldelight.model

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.NameAllocator
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.WildcardTypeName
import com.squareup.sqldelight.FactorySpec
import com.squareup.sqldelight.FactorySpec.Companion.FACTORY_NAME
import com.squareup.sqldelight.SqliteCompiler
import com.squareup.sqldelight.SqliteCompiler.Companion.NON_NULL
import com.squareup.sqldelight.SqliteCompiler.Companion.NULLABLE
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.resolution.query.Value
import com.squareup.sqldelight.types.Argument
import com.squareup.sqldelight.types.ArgumentType
import com.squareup.sqldelight.types.ArgumentType.SetOfValues
import com.squareup.sqldelight.types.SqliteType
import com.squareup.sqldelight.types.toSqliteArguments
import com.squareup.sqldelight.util.javadocText
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.misc.Interval
import java.util.Collections
import javax.lang.model.element.Modifier
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PUBLIC
import kotlin.comparisons.compareBy

class SqlStmt private constructor(
    unorderedArguments: List<Argument>,
    val statement: ParserRuleContext,
    val name: String,
    val javadoc: String?,
    val tablesUsed: Set<String> = emptySet()
) {
  val arguments: List<Argument>
  val sqliteText: String
  val programName = name.capitalize()
  val needsConstant: Boolean
  val needsSqlDelightStatement: Boolean
  val needsCompiledStatement: Boolean

  internal constructor(
      arguments: List<Argument>,
      statement: SqliteParser.Create_table_stmtContext
  ) : this(arguments, statement, SqliteCompiler.CREATE_TABLE, null)

  internal constructor(
      arguments: List<Argument>,
      statement: SqliteParser.Sql_stmtContext,
      tablesUsed: Set<String>
  ) : this(
      arguments,
      statement.getChild(statement.childCount - 1) as ParserRuleContext,
      statement.sql_stmt_name().text,
      statement.javadocText(),
      tablesUsed
  )

  init {
    // Modify argument ranges to index from the beginning of the sqlite statement.
    val orderedArguments = unorderedArguments.toSqliteArguments()
    orderedArguments.forEach {
      it.ranges.forEachIndexed { i, range ->
        it.ranges[i] = IntRange(range.start - statement.start.startIndex, range.endInclusive - statement.start.startIndex)
      }
    }

    val text = statement.textWithWhitespace()
    var nextOffset = 0
    sqliteText = statement.replacements()
        .fold(StringBuilder(), { builder, replacement ->
          builder.append(text.subSequence(nextOffset, replacement.startOffset - statement.start.startIndex))
          nextOffset = replacement.endOffset - statement.start.startIndex

          // Any arguments that occur after the current text block need to be modified with
          // respect to the replacement text length.
          val diff = replacement.replacementText.length - (replacement.endOffset - replacement.startOffset)
          orderedArguments.forEach {
            it.ranges.forEachIndexed { i, range ->
              if (range.start > builder.length) {
                // This range comes after the replacement and needs to account for the replacement.
                it.ranges[i] = IntRange(range.start + diff, range.endInclusive + diff)
              }
            }
          }
          builder.append(replacement.replacementText)
        })
        .append(text.substring(nextOffset, text.length))
        .toString()

    this.arguments = orderedArguments

    needsSqlDelightStatement = arguments.isNotEmpty() || statement is SqliteParser.Select_stmtContext
    needsCompiledStatement = statement !is SqliteParser.Select_stmtContext
        && (arguments.isNotEmpty() || statement.isMutatorStatement())
        && arguments.none { it.argumentType is ArgumentType.SetOfValues }
    needsConstant = !needsSqlDelightStatement && !needsCompiledStatement
  }

  private fun ParserRuleContext.isMutatorStatement() =
      this is SqliteParser.Delete_stmtContext
          || this is SqliteParser.Insert_stmtContext
          || this is SqliteParser.Update_stmtContext

  internal fun programClass(): TypeSpec {
    val type = TypeSpec.classBuilder(programName)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .superclass(SQLDELIGHT_STATEMENT)

    if (javadoc != null) {
      type.addJavadoc(javadoc)
    }

    val constructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(SQLITEDATABASE_TYPE, "database")
        .addStatement("super(\$S, database.compileStatement(\"\"\n    + \$S))",
            tablesUsed.first(), sqliteText)

    arguments.mapNotNull { it.argumentType.comparable }
        .filter { !it.isHandledType && it.tableInterface != null }
        .distinctBy { it.tableInterface }
        .forEach {
          val factoryType = ParameterizedTypeName.get(
              it.tableInterface!!.nestedClass(FactorySpec.FACTORY_NAME),
              WildcardTypeName.subtypeOf(it.tableInterface)
          )
          type.addField(factoryType, it.factoryField(), Modifier.PRIVATE, Modifier.FINAL)
          constructor.addParameter(factoryType, it.factoryField())
              .addStatement("this.${it.factoryField()} = ${it.factoryField()}")
        }

    return type.addMethod(constructor.build())
        .apply { if (arguments.isNotEmpty()) addMethod(programMethod()) }
        .build()
  }

  internal fun programMethod(): MethodSpec {
    val method = MethodSpec.methodBuilder("bind")
        .addModifiers(Modifier.PUBLIC)

    arguments.sortedBy { it.index }.forEach { argument ->
      val parameter = ParameterSpec.builder(
          argument.argumentType.comparable?.javaType ?: TypeName.OBJECT, argument.name)
      if (argument.argumentType.comparable != null) {
        parameter.addAnnotations(argument.argumentType.comparable.annotations())
      }
      method.addParameter(parameter.build())

      var startedControlFlow = false
      if (argument.argumentType.comparable == null || argument.argumentType.comparable.nullable) {
        method.beginControlFlow("if (${argument.name} == null)")
            .addStatement("bindNull(${argument.index})")
        startedControlFlow = true
      }
      if (argument.argumentType.comparable == null) {
        method.nextControlFlow("else if (${argument.name} instanceof String)")
            .addStatement("bindString(${argument.index}, (String) ${argument.name})")
            .nextControlFlow(
                "else if (${argument.name} instanceof \$T || ${argument.name} instanceof \$T)",
                TypeName.FLOAT.box(),
                TypeName.DOUBLE.box()
            )
            .addStatement("bindDouble(${argument.index}, (double) ${argument.name})")
            .nextControlFlow(
                "else if (${argument.name} instanceof \$T" +
                " || ${argument.name} instanceof \$T" +
                " || ${argument.name} instanceof \$T)",
                TypeName.INT.box(),
                TypeName.SHORT.box(),
                TypeName.LONG.box()
            )
            .addStatement("bindLong(${argument.index}, (long) ${argument.name})")
            .nextControlFlow("else if (${argument.name} instanceof \$T)", TypeName.BOOLEAN.box())
            .addStatement("bindLong(${argument.index}, (boolean) ${argument.name} ? 1 : 0)")
            .nextControlFlow("else if (${argument.name} instanceof \$T)", ArrayTypeName.of(TypeName.BYTE))
            .addStatement("bindBlob(${argument.index}, (byte[]) ${argument.name})")
            .nextControlFlow("else")
            .addStatement(
                "throw new \$T(\"Attempting to bind an object that is not one of" +
                " (String, Integer, Short, Long, Float, Double, Boolean, byte[]) to argument" +
                " ${argument.name}\")",
                ClassName.get(IllegalArgumentException::class.java)
            )
      } else {
        if (startedControlFlow) method.nextControlFlow("else")
        method.addStatement(argument.argumentType.comparable.bindMethod() +
            "(${argument.index}, ${argument.getter()})")
      }
      if (startedControlFlow) method.endControlFlow()
    }
    return method.build()
  }

  private fun Value.bindMethod() = when (dataType) {
    SqliteType.BLOB -> "bindBlob"
    SqliteType.INTEGER -> "bindLong"
    SqliteType.NULL -> "bindNull"
    SqliteType.REAL -> "bindDouble"
    SqliteType.TEXT -> "bindString"
  }

  private fun createTableSet() = if (tablesUsed.isEmpty()) {
    CodeBlock.of("\$T.<\$T>emptySet()", COLLECTIONS_TYPE, String::class.java)
  } else {
    val tableTemplate = Collections.nCopies(tablesUsed.size, "\$S").joinToString(", ")
    CodeBlock.of("new \$T($tableTemplate)", SQLDELIGHT_SET, *tablesUsed.toTypedArray())
  }

  /**
   * Create a [CodeBlock] from [sqliteText] so that it can be emitted as string literal,
   * transforming it to normalize the arguments and accommodate argument sets.
   *
   * - Single '?'s representing sets of values are replaced with a call to a factory method which
   *   produces a single '?' for each value.
   * - Named, indexed, or unindexed arguments are replaced with normalized
   *
   * For example, `"foo IN ?"` becomes `"foo IN " + QuestionMarks.ofSize(whatever.length)`.
   *
   * @param nameAllocator A [NameAllocator] in which each argument instance has been registered.
   */
  private fun sqlString(nameAllocator: NameAllocator): CodeBlock {
    check(arguments.isNotEmpty()) { "This method may only be called when arguments are used." }

    val sql = CodeBlock.builder()

    var requiresConcatenation = false
    var lastEnd = 0
    var staticSql = ""
    val sortedArguments = arguments.sortedWith(argumentBindComparator)
    arguments.flatMap { argument -> argument.ranges.map { argument to it } }
        .sortedBy { it.second.start }
        .forEach { pair ->
          val (argument, range) = pair // TODO destructuring lambda argument after Kotlin update.

          // Append the non-argument SQL text up until this argument.
          staticSql += sqliteText.substring(lastEnd, range.start)

          if (argument.argumentType is SetOfValues) {
            if (requiresConcatenation) {
              sql.add(" + ")
            }
            requiresConcatenation = true

            val name = nameAllocator.get(argument)
            // Output the SQL string up until this point followed by the question mark factory.
            sql.add("\$S + \$T.ofSize(\$N.length)", staticSql, SQLDELIGHT_QUESTION_MARKS, name)

            staticSql = ""
          } else {
            // For non-set arguments, normalize the argument index based on the natural ordering of
            // the arguments. This will match the parameter order and thus the binding order.
            val normalizedIndex = sortedArguments.indexOf(argument) + 1
            staticSql += "?" + normalizedIndex
          }

          lastEnd = range.endInclusive + 1
        }

    // Ensure we account for trailing, non-argument SQL after the last argument position.
    if (lastEnd < sqliteText.length) {
      staticSql += sqliteText.substring(lastEnd)
    }

    if (staticSql.isNotEmpty()) {
      if (requiresConcatenation) {
        sql.add(" + ")
      }
      sql.add("\$S", staticSql)
    }

    return sql.build()
  }

  /** Return the foreign factory types required to bind the arguments of this statement. */
  private fun foreignFactories(tableInterfaceName: ClassName) = arguments
      .mapNotNull { it.argumentType.comparable }
      .filter { !it.isHandledType && it.tableInterface != null && it.tableInterface != tableInterfaceName }
      .distinctBy { it.tableInterface }
      .associate {
        val rawFactory = it.tableInterface!!.nestedClass(FACTORY_NAME)
        val factory = ParameterizedTypeName.get(rawFactory, WildcardTypeName.subtypeOf(it.tableInterface))
        return@associate it.factoryField() to factory
      }

  internal fun factoryQueryMethod(tableInterfaceName: ClassName, queryTypeName: ClassName): MethodSpec {
    val foreignFactories = foreignFactories(tableInterfaceName)

    val nameAllocator = NameAllocator()
    arguments.forEach {
      nameAllocator.newName(it.name, it)
    }
    foreignFactories.keys.forEach {
      nameAllocator.newName(it, it)
    }

    val method = MethodSpec.methodBuilder(name)
        .addModifiers(Modifier.PUBLIC)
        .returns(SQLDELIGHT_QUERY)

    javadoc?.let { method.addJavadoc(it) }

    if (arguments.isNotEmpty()) {
      val argumentNames = mutableListOf<String>()

      foreignFactories.forEach {
        val (factoryName, factoryType) = it // TODO destructuring lambda argument after Kotlin update.
        val name = nameAllocator.get(factoryName)
        argumentNames.add(name)

        // The first arguments to the method will be any foreign factories needed.
        method.addParameter(ParameterSpec.builder(factoryType, name)
            .addAnnotation(NON_NULL)
            .build())
      }

      arguments.sortedBy { it.index }.forEach { argument ->
        val isSetOfValues = argument.argumentType is SetOfValues
        val rawType = argument.argumentType.comparable?.javaType ?: TypeName.OBJECT
        val javaType = if (isSetOfValues) ArrayTypeName.of(rawType) else rawType
        val annotations = argument.argumentType.comparable?.annotations()
            ?: listOf(AnnotationSpec.builder(NULLABLE).build())
        val name = nameAllocator.get(argument)
        argumentNames.add(name)

        method.addParameter(ParameterSpec.builder(javaType, name)
            .addAnnotations(annotations)
            .build())
      }

      val args = CodeBlock.of(argumentNames.joinToString(",\$W") { "\$N" }, *argumentNames.toTypedArray())
      method.addStatement("return new \$T(\$L)", queryTypeName, args)
    } else {
      method.addStatement("return new \$T(\"\"\n+ \$<\$<\$S\$>\$>,\n\$L)", SQLDELIGHT_QUERY,
          sqliteText, createTableSet())
    }
    return method.build()
  }

  internal fun factoryQueryType(tableInterfaceName: ClassName, queryClassName: ClassName): TypeSpec? {
    if (arguments.isEmpty()) {
      return null // No custom type needed. We can use the base type directly.
    }

    val foreignFactories = foreignFactories(tableInterfaceName)

    val nameAllocator = NameAllocator()
    arguments.forEach {
      nameAllocator.newName(it.name, it)
    }
    foreignFactories.keys.forEach {
      nameAllocator.newName(it, it)
    }
    val program = nameAllocator.newName("program")
    val nextIndex = nameAllocator.newName("nextIndex")
    val forEachItem = nameAllocator.newName("item")

    val type = TypeSpec.classBuilder(queryClassName)
        .addModifiers(PRIVATE, FINAL)
        .superclass(SQLDELIGHT_QUERY)

    val constructor = MethodSpec.constructorBuilder()
        .addStatement("super(\$<\$<\$L\$>\$>,\n\$L)", sqlString(nameAllocator), createTableSet())
        .addCode("\n")

    foreignFactories.forEach {
      val (factoryName, factoryType) = it // TODO destructuring lambda argument after Kotlin update.
      val name = nameAllocator.get(factoryName)

      type.addField(FieldSpec.builder(factoryType, name, PRIVATE, FINAL)
          .addAnnotation(NON_NULL)
          .build())

      // The first arguments to the constructor will be any foreign factories needed.
      constructor.addParameter(ParameterSpec.builder(factoryType, name)
          .addAnnotation(NON_NULL)
          .build())

      constructor.addStatement("this.\$1N = \$1N", name)
    }

    arguments.sortedBy { it.index }.forEach { argument ->
      val isSetOfValues = argument.argumentType is SetOfValues
      val name = nameAllocator.get(argument)
      val rawType = argument.argumentType.comparable?.javaType ?: TypeName.OBJECT
      val javaType = if (isSetOfValues) ArrayTypeName.of(rawType) else rawType
      val annotations = argument.argumentType.comparable?.annotations()
          ?: listOf(AnnotationSpec.builder(NULLABLE).build())

      type.addField(FieldSpec.builder(javaType, name, PRIVATE, FINAL)
          .addAnnotations(annotations)
          .build())

      constructor.addParameter(ParameterSpec.builder(javaType, name)
          .addAnnotations(annotations)
          .build())

      constructor.addStatement("this.\$1N = \$1N", name)
    }

    val bindTo = MethodSpec.methodBuilder("bindTo")
        .addAnnotation(Override::class.java)
        .addModifiers(PUBLIC)
        .addParameter(SQLITEPROGAM_TYPE, program)

    var seenSetOfValues = false
    arguments.sortedWith(argumentBindComparator).forEachIndexed { argumentIndex, argument ->
      if (argumentIndex > 0) {
        bindTo.addCode("\n")
      }

      val name = nameAllocator.get(argument)
      val isSetOfValues = argument.argumentType is SetOfValues
      val argumentValue = argument.argumentType.comparable
      val itemName = if (isSetOfValues) forEachItem else name
      val rawType = argumentValue?.javaType ?: TypeName.OBJECT
      val javaType = if (isSetOfValues) ArrayTypeName.of(rawType) else rawType
      val isNullable = argumentValue?.nullable ?: true
      val programIndex = argumentIndex + 1

      // Use exact indices as long as possible. Switch to a dynamic index upon first set of values.
      if (!seenSetOfValues && isSetOfValues) {
        seenSetOfValues = true
        bindTo.addStatement("int \$N = \$L", nextIndex, programIndex)
            .addCode("\n")
      }
      val index = if (seenSetOfValues) {
        CodeBlock.of("\$N++", nextIndex)
      } else {
        CodeBlock.of("\$L", programIndex)
      }

      if (isNullable) {
        // Cache the field in a local since we will be referring to it more than once.
        bindTo.addStatement("\$1T \$2N = this.\$2N", javaType, name)
            .beginControlFlow("if (\$N != null)", name)
      }
      if (isSetOfValues) {
        bindTo.beginControlFlow("for (\$T \$N : \$N)", rawType, forEachItem, name)
      }

      if (rawType.box() == TypeName.BOOLEAN.box()) {
        bindTo.addStatement("\$N.bindLong(\$L, \$N ? 1 : 0)", program, index, itemName)
      } else if (argumentValue != null) {
        val bindMethod = argumentValue.bindMethod()
        val bindValue = if (argumentValue.isHandledType) {
          CodeBlock.of("\$N", itemName)
        } else {
          CodeBlock.of("\$L", argument.serializer(itemName, tableInterfaceName))
        }
        bindTo.addStatement("\$N.\$N(\$L, \$L)", program, bindMethod, index, bindValue)
      } else {
        // TODO Do we even want to support passing in Object?
        bindTo.beginControlFlow("if (\$N instanceof \$T)", itemName, String::class.java)
            .addStatement("\$N.bindString(\$L, (\$T) \$N)", program, index, String::class.java, itemName)
            .nextControlFlow("else if (\$1N instanceof \$2T || \$1N instanceof \$3T || \$1N instanceof \$4T)", itemName, Long::class.javaObjectType, Integer::class.javaObjectType, Short::class.javaObjectType)
            .addStatement("\$N.bindLong(\$L, (long) \$N)", program, index, itemName)
            .nextControlFlow("else if (\$N instanceof \$T)", itemName, Boolean::class.javaObjectType)
            .addStatement("\$N.bindLong(\$L, (boolean) \$N ? 0 : 1)", program, index, itemName)
            .nextControlFlow("else if (\$N instanceof byte[])", itemName)
            .addStatement("\$N.bindBlob(\$L, (byte[]) \$N)", program, index, itemName)
            .nextControlFlow("else if (\$1N instanceof \$2T || \$1N instanceof \$3T)", itemName, Float::class.javaObjectType, Double::class.javaObjectType)
            .addStatement("\$N.bindDouble(\$L, (double) \$N)", program, index, itemName)
            .nextControlFlow("else")
            .addStatement("throw new \$T(\$S)", IllegalArgumentException::class.java, "Attempting to bind an object that is not one of String, Integer, Short, Long, Float, Double, Boolean, or byte[] to argument ${argument.name}")
            .endControlFlow()
      }

      if (isSetOfValues) {
        bindTo.endControlFlow()
      }
      if (isNullable) {
        bindTo.nextControlFlow("else")
            .addStatement("\$N.bindNull(\$L)", program, index)
            .endControlFlow()
      }
    }

    return type
        .addMethod(constructor.build())
        .addMethod(bindTo.build())
        .build()
  }

  /**
   * Within a method the getter represents the actual value we want to append to the sqlite
   * text or add to the args list.
   */
  private fun Argument.getter(tableInterfaceName: ClassName? = null): String {
    val argName = if (argumentType is ArgumentType.SingleValue) name!! else "$name[i]"
    return serializer(argName, tableInterfaceName)
  }

  private fun Argument.serializer(name: String, tableInterfaceName: ClassName?): String {
    // TODO This should return a CodeBlock.
    return if (argumentType.comparable?.javaType == TypeName.BOOLEAN
        || argumentType.comparable?.javaType == TypeName.BOOLEAN.box()) {
      "$name ? 1 : 0"
    } else if (argumentType.comparable?.isHandledType != false) {
      name
    } else argumentType.comparable!!.let { value ->
      if (value.tableInterface == tableInterfaceName) {
        "${value.adapterField}.encode($name)"
      } else {
        "${value.factoryField()}.${value.adapterField}.encode($name)"
      }
    }
  }

  companion object {
    val SQLDELIGHT_STATEMENT = ClassName.get("com.squareup.sqldelight", "SqlDelightStatement")
    val SQLDELIGHT_QUERY = ClassName.get("com.squareup.sqldelight", "SqlDelightQuery")
    val SQLDELIGHT_QUESTION_MARKS = ClassName.get("com.squareup.sqldelight.internal", "QuestionMarks")
    val SQLDELIGHT_SET = ClassName.get("com.squareup.sqldelight.internal", "TableSet")
    val SQLITEDATABASE_TYPE = ClassName.get("android.arch.persistence.db", "SupportSQLiteDatabase")
    val SQLITEPROGAM_TYPE = ClassName.get("android.arch.persistence.db", "SupportSQLiteProgram")
    val COLLECTIONS_TYPE = ClassName.get(Collections::class.java)
  }
}

fun ParserRuleContext.textWithWhitespace(): String {
  return if (start == null || stop == null || start.startIndex < 0 || stop.stopIndex < 0) text
  else start.inputStream.getText(Interval(start.startIndex, stop.stopIndex))
}

internal val SqliteParser.Sql_stmtContext.identifier: String
  get() = SqliteCompiler.constantName(sql_stmt_name().text)

fun ParserRuleContext.sqliteText(): String {
  val text = textWithWhitespace()
  var nextOffset = 0
  return replacements()
      .fold(StringBuilder(), { builder, replacement ->
        builder.append(text.subSequence(nextOffset, replacement.startOffset - start.startIndex))
            .append(replacement.replacementText)
        nextOffset = replacement.endOffset - start.startIndex
        builder
      })
      .append(text.substring(nextOffset, text.length))
      .toString()
}

private fun ParserRuleContext.replacements(): Collection<Replacement> {
  if (this is SqliteParser.Type_nameContext && K_AS() != null) {
    return listOf(Replacement(K_AS().symbol.startIndex - 1, java_type_name().stop.stopIndex + 1, ""))
  }
  if (this is SqliteParser.Sql_stmtContext) {
    return listOf(Replacement(
        sql_stmt_name().start.startIndex,
        (getChild(2) as? ParserRuleContext)?.start?.startIndex ?: sql_stmt_name().stop.stopIndex,
        ""
    ))
  }
  var replacements = emptyList<Replacement>()
  if (this is SqliteParser.Create_table_stmtContext && JAVADOC_COMMENT() != null) {
    replacements += Replacement(JAVADOC_COMMENT().symbol.startIndex, K_CREATE().symbol.startIndex, "")
  }
  if (this is SqliteParser.Column_defContext && JAVADOC_COMMENT() != null) {
    replacements += Replacement(JAVADOC_COMMENT().symbol.startIndex, column_name().start.startIndex, "")
  }
  if (children != null) replacements += children.filterIsInstance<ParserRuleContext>().flatMap { it.replacements() }.toList()
  return replacements
}

private class Replacement(val startOffset: Int, val endOffset: Int, val replacementText: String)

internal fun SqliteParser.Sql_stmtContext.javadocText(): String? {
  return javadocText(JAVADOC_COMMENT())
}

/**
 * A [Comparator] which prefers non-set arguments and then any specified indices. This allows
 * code generation to use a static index for the majority of arguments and only switch to a dynamic
 * one at the end for any set arguments.
 */
private val argumentBindComparator = compareBy<Argument>(
    { it.argumentType is SetOfValues }, // Sets go last (false sorts above true).
    { it.index } // Declared indices are honored.
)
