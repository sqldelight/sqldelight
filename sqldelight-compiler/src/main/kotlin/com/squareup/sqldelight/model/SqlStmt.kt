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

import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.WildcardTypeName
import com.squareup.sqldelight.FactorySpec
import com.squareup.sqldelight.SqliteCompiler
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.resolution.query.Value
import com.squareup.sqldelight.types.Argument
import com.squareup.sqldelight.types.ArgumentType
import com.squareup.sqldelight.types.SqliteType
import com.squareup.sqldelight.types.SqliteType.TEXT
import com.squareup.sqldelight.types.toSqliteArguments
import com.squareup.sqldelight.util.javadocText
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.misc.Interval
import java.util.ArrayList
import java.util.Collections
import javax.lang.model.element.Modifier

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
        .superclass(SQLDELIGHT_COMPILED_STATEMENT)

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
            .addStatement("program.bindNull(${argument.index})")
        startedControlFlow = true
      }
      if (argument.argumentType.comparable == null) {
        method.nextControlFlow("else if (${argument.name} instanceof String)")
            .addStatement("program.bindString(${argument.index}, (String) ${argument.name})")
            .nextControlFlow(
                "else if (${argument.name} instanceof \$T || ${argument.name} instanceof \$T)",
                TypeName.FLOAT.box(),
                TypeName.DOUBLE.box()
            )
            .addStatement("program.bindDouble(${argument.index}, (double) ${argument.name})")
            .nextControlFlow(
                "else if (${argument.name} instanceof \$T" +
                " || ${argument.name} instanceof \$T" +
                " || ${argument.name} instanceof \$T)",
                TypeName.INT.box(),
                TypeName.SHORT.box(),
                TypeName.LONG.box()
            )
            .addStatement("program.bindLong(${argument.index}, (long) ${argument.name})")
            .nextControlFlow("else if (${argument.name} instanceof \$T)", TypeName.BOOLEAN.box())
            .addStatement("program.bindLong(${argument.index}, (boolean) ${argument.name} ? 1 : 0)")
            .nextControlFlow("else if (${argument.name} instanceof \$T)", ArrayTypeName.of(TypeName.BYTE))
            .addStatement("program.bindBlob(${argument.index}, (byte[]) ${argument.name})")
            .nextControlFlow("else")
            .addStatement(
                "throw new \$T(\"Attempting to bind an object that is not one of" +
                " (String, Integer, Short, Long, Float, Double, Boolean, byte[]) to argument" +
                " ${argument.name}\")",
                ClassName.get(IllegalArgumentException::class.java)
            )
      } else {
        if (startedControlFlow) method.nextControlFlow("else")
        method.addStatement("program.${argument.argumentType.comparable.bindMethod()}" +
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

  internal fun factoryQueryMethod(): MethodSpec {
    // Note: at present this method is only called when arguments.isEmpty() and we assume such!

    val method = MethodSpec.methodBuilder(name)
        .addModifiers(Modifier.PUBLIC)
        .returns(SQLDELIGHT_QUERY)

    javadoc?.let { method.addJavadoc(it) }

    method.addStatement("return new \$T(\"\"\n+ \$<\$<\$S\$>\$>,\n\$L)", SQLDELIGHT_QUERY,
        sqliteText, createTableSet())
    return method.build()
  }

  internal fun factoryStatementMethod(factoryClass: ClassName, addFactories: Boolean): MethodSpec {
    // Note: at present this method is only called when arguments.isNotEmpty() and we assume such!

    val method = MethodSpec.methodBuilder(name)
        .addModifiers(Modifier.PUBLIC)
        .returns(SQLDELIGHT_STATEMENT)

    javadoc?.let { method.addJavadoc(it) }

    if (addFactories) {
      // The first arguments to the method will be any foreign factories needed.
      arguments.mapNotNull { it.argumentType.comparable }
          .filter { !it.isHandledType && it.tableInterface != null && it.tableInterface != factoryClass }
          .distinctBy { it.tableInterface }
          .forEach {
            method.addParameter(ParameterizedTypeName.get(
                it.tableInterface!!.nestedClass("Factory"), WildcardTypeName.subtypeOf(it.tableInterface)
            ), it.factoryField())
          }
    }

    // Subsequent arguments are the actual bind args for the query.
    arguments.forEach {
      var type = it.argumentType.comparable?.javaType ?: TypeName.OBJECT
      if (it.argumentType is ArgumentType.SetOfValues) type = ArrayTypeName.of(type)
      val parameter = ParameterSpec.builder(type, it.name)
      if (it.argumentType.comparable != null) {
        parameter.addAnnotations(it.argumentType.comparable.annotations())
      }
      method.addParameter(parameter.build())
    }

    val argsCodeBlock = if (arguments.any { it.argumentType.comparable == null || it.argumentType.comparable.dataType == TEXT }) {
      // Method body begins with local vars used during computation.
      method.addStatement("\$1T<\$3T> args = new \$2T<\$3T>()", LIST_TYPE, ARRAYLIST_TYPE, Object::class.java)
          .addStatement("int currentIndex = 1")

      CodeBlock.of("args.toArray(new \$T[args.size()])", Object::class.java)
    } else {
      CodeBlock.of("new \$T[0]", Object::class.java)
    }

    method.addStatement("\$1T query = new \$1T()", STRINGBUILDER_TYPE)

    var lastEnd = 0
    arguments.flatMap { argument -> argument.ranges.map { it to argument } }
        .sortedBy { it.first.start }
        .forEach { pair ->
          val (range, argument) = pair
          method.addStatement("query.append(\$S)", sqliteText.substring(lastEnd until range.start))
          if (argument.argumentType.comparable?.dataType == SqliteType.TEXT && argument.ranges.size > 1
              && range == argument.ranges[0]) {
            // Store the sqlite index for later range replacements.
            if (argument.argumentType is ArgumentType.SingleValue) {
              method.addStatement("int arg${argument.index}Index = currentIndex")
            } else {
              method.addStatement("int arg${argument.index}Index[] = new int[${argument.name}.length]")
            }
          }

          val replacementCode = CodeBlock.builder()
          if (argument.argumentType is ArgumentType.SingleValue) {
            var startedControlFlow = false

            if (argument.argumentType.comparable?.nullable ?: false) {
              startedControlFlow = true
              // First check if the argument is null.
              replacementCode.beginControlFlow("if (${argument.name} == null)")
                  .addStatement("query.append(\"null\")")
            }

            if (argument.argumentType.comparable == null) {
              // Then check if the argument is not a string.
              val conditional = "!(${argument.name} instanceof String)" // TODO should use $T
              if (startedControlFlow) {
                replacementCode.nextControlFlow("else if ($conditional)")
              } else {
                startedControlFlow = true
                replacementCode.beginControlFlow("if ($conditional)")
              }
              replacementCode.addStatement("query.append(${argument.name})")
            }

            if (startedControlFlow) replacementCode.nextControlFlow("else")

            val dataType = argument.argumentType.comparable?.dataType
            if (dataType == null || dataType == SqliteType.TEXT) {
              var argumentGetter = argument.getter(factoryClass)
              if (argument.argumentType.comparable == null || !argument.argumentType.comparable.isHandledType) {
                argumentGetter = "(String) $argumentGetter" // TODO should use $T
              }
              // Argument is a String
              if (range == argument.ranges[0]) {
                // The first occurence of this arg needs to increment the current index.
                replacementCode.addStatement("query.append(\'?\').append(currentIndex++)")
                    .addStatement("args.add($argumentGetter)")
              } else {
                // Subsequent occurences of the arg should use the stored index.
                replacementCode.addStatement(
                    "query.append(\'?\').append(arg${argument.index}Index)")
              }
            } else if (dataType == SqliteType.BLOB) {
              replacementCode.addStatement(
                  "query.append(\$T.forBlob(${argument.getter(factoryClass)}))",
                  SQLDELIGHT_LITERALS)
            } else {
              // Argument is a non-string type.
              replacementCode.addStatement("query.append(${argument.getter(factoryClass)})")
            }

            if (startedControlFlow) replacementCode.endControlFlow()
          } else if (argument.argumentType is ArgumentType.SetOfValues) {
            replacementCode.addStatement("query.append(\'(\')")
                .beginControlFlow("for (int i = 0; i < ${argument.name}.length; i++)")
                .addStatement("if (i != 0) query.append(\", \")")

            val dataType = argument.argumentType.comparable?.dataType
            if (dataType == SqliteType.TEXT) {
              // Text args use sqlite bind args.
              if (argument.ranges.size > 1) {
                // Using stored indices.
                replacementCode.add("query.append(\'?\').append(arg${argument.index}Index[i]")
                if (range == argument.ranges[0]) {
                  replacementCode.addStatement(" = currentIndex++)")
                      .addStatement("args.add(${argument.getter(factoryClass)})")
                } else {
                  replacementCode.addStatement(")")
                }
              } else {
                // No need to store indices. Still requires increment of the index.
                replacementCode.addStatement("query.append(\'?\').append(currentIndex++)")
                    .addStatement("args.add(${argument.getter(factoryClass)})")
              }
            } else if (dataType == SqliteType.BLOB) {
              replacementCode.addStatement(
                  "query.append(\$T.forBlob(${argument.getter(factoryClass)}))",
                  SQLDELIGHT_LITERALS)
            } else {
              // Other types append directly.
              replacementCode.addStatement("query.append(${argument.getter(factoryClass)})")
            }

            replacementCode.endControlFlow()
                .addStatement("query.append(\')\')")
          }

          method.addCode(replacementCode.build())
          lastEnd = range.endInclusive+1
        }

    sqliteText.substring(lastEnd).let {
      if (it.isNotEmpty()) method.addStatement("query.append(\$S)", it)
    }

    method.addStatement("return new \$T(query.toString(), \$L, \$L)", SQLDELIGHT_STATEMENT, argsCodeBlock, createTableSet())

    return method.build()
  }

  /**
   * Within a method the getter represents the actual value we want to append to the sqlite
   * text or add to the args list.
   */
  private fun Argument.getter(factoryClass: ClassName? = null): String {
    val argName = if (argumentType is ArgumentType.SingleValue) name!! else "$name[i]"
    return if (argumentType.comparable?.javaType == TypeName.BOOLEAN ||
        argumentType.comparable?.javaType == TypeName.BOOLEAN.box()) {
      "$argName ? 1 : 0"
    } else if (argumentType.comparable?.isHandledType ?: true) {
      argName
    } else argumentType.comparable!!.let { value ->
      if (value.tableInterface == factoryClass) {
        "${value.adapterField}.encode($argName)"
      } else {
        "${value.factoryField()}.${value.adapterField}.encode($argName)"
      }
    }
  }

  companion object {
    val SQLDELIGHT_COMPILED_STATEMENT = ClassName.get("com.squareup.sqldelight", "SqlDelightCompiledStatement")
    val SQLDELIGHT_STATEMENT = ClassName.get("com.squareup.sqldelight", "SqlDelightStatement")
    val SQLDELIGHT_QUERY = ClassName.get("com.squareup.sqldelight", "SqlDelightQuery")
    val SQLDELIGHT_LITERALS = ClassName.get("com.squareup.sqldelight.internal", "SqliteLiterals")
    val SQLDELIGHT_SET = ClassName.get("com.squareup.sqldelight.internal", "TableSet")
    val SQLITEDATABASE_TYPE = ClassName.get("android.arch.persistence.db", "SupportSQLiteDatabase")
    val LIST_TYPE = ClassName.get(List::class.java)
    val ARRAYLIST_TYPE = ClassName.get(ArrayList::class.java)
    val STRINGBUILDER_TYPE = ClassName.get(StringBuilder::class.java)
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
