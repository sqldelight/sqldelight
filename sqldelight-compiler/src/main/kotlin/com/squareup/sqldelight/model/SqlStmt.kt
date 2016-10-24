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
import com.squareup.sqldelight.types.toSqliteArguments
import com.squareup.sqldelight.util.javadocText
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.misc.Interval
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.LinkedHashSet
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

    needsConstant = statement is SqliteParser.Select_stmtContext
        || arguments.isEmpty()
        || arguments.any { it.argumentType is ArgumentType.SetOfValues }
  }

  private fun unmodifiableListOfTables() = CodeBlock.of("\$T.<\$T>unmodifiableSet(" +
        "new \$T<\$T>(\$T.asList(${tablesUsed.joinToString("\",\"", "\"", "\"")}))" +
      ")",
      COLLECTIONS_TYPE, STRING_TYPE, LINKEDHASHSET_TYPE, STRING_TYPE, ARRAYS_TYPE)

  internal fun programClass(): TypeSpec {
    val type = TypeSpec.classBuilder(programName)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .superclass(when (statement) {
          is SqliteParser.Insert_stmtContext -> SQLDELIGHT_INSERT_STATEMENT
          is SqliteParser.Update_stmtContext -> SQLDELIGHT_UPDATE_STATEMENT
          is SqliteParser.Delete_stmtContext -> SQLDELIGHT_DELETE_STATEMENT
          else -> SQLDELIGHT_COMPILED_STATEMENT
        })

    val constructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(SQLITEDATABASE_TYPE, "database")
        .addStatement("super(\$S, database.compileStatement(\"\"\n    + \$S))",
            tablesUsed.first(), sqliteText)

    arguments.map { it.argumentType.comparable }
        .filterNotNull()
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
        .addMethod(programMethod())
        .build()
  }

  internal fun programMethod(): MethodSpec {
    val method = MethodSpec.methodBuilder("bind")
        .addModifiers(Modifier.PUBLIC)

    arguments.forEach { argument ->
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

  internal fun factoryStatementMethod(factoryClass: ClassName, addFactories: Boolean): MethodSpec {
    val method = MethodSpec.methodBuilder(name)
        .addModifiers(Modifier.PUBLIC)
        .returns(SQLDELIGHT_STATEMENT)

    if (addFactories) {
      // The first arguments to the method will be any foreign factories needed.
      arguments.map { it.argumentType.comparable }
          .filterNotNull()
          .filter { !it.isHandledType && it.tableInterface != null && it.tableInterface != factoryClass }
          .distinctBy { it.tableInterface }
          .forEach {
            method.addParameter(it.tableInterface!!.nestedClass("Factory"), it.factoryField())
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

    // Method body begins with local vars used during computation.
    method.addStatement("\$T<String> args = new \$T<String>()", LIST_TYPE, ARRAYLIST_TYPE)
        .addStatement("int currentIndex = 1")
        .addStatement("\$1T query = new \$1T()", STRINGBUILDER_TYPE)

    var lastEnd = 0
    arguments.flatMap { argument -> argument.ranges.map { it to argument } }
        .sortedBy { it.first.start }
        .forEach { pair ->
          val (range, argument) = pair
          method.addStatement("query.append(\$S)", sqliteText.substring(lastEnd..range.start-1))
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
              val conditional = "!(${argument.name} instanceof String)"
              if (startedControlFlow) {
                replacementCode.nextControlFlow("else if ($conditional)")
              } else {
                startedControlFlow = true
                replacementCode.beginControlFlow("if ($conditional)")
              }
              replacementCode.addStatement("query.append(${argument.name})")
            }

            if (startedControlFlow) replacementCode.nextControlFlow("else")

            if (argument.argumentType.comparable == null || argument.argumentType.comparable.dataType == SqliteType.TEXT) {
              var argumentGetter = argument.getter(factoryClass)
              if (argument.argumentType.comparable == null || !argument.argumentType.comparable.isHandledType) {
                argumentGetter = "(String) $argumentGetter"
              }
              // Argument is a String
              if (range == argument.ranges[0]) {
                // The first occurence of this arg needs to increment the current index.
                replacementCode.addStatement("query.append(\'?\').append(currentIndex++)")
                    .addStatement("args.add($argumentGetter)")
              } else {
                // Subsequent occurences of the arg should use the stored index.
                replacementCode.addStatement("query.append(\'?\').append(arg${argument.index}Index)")
              }
            } else {
              // Argument is a non-string type.
              replacementCode.addStatement("query.append(${argument.getter(factoryClass)})")
            }

            if (startedControlFlow) replacementCode.endControlFlow()
          } else if (argument.argumentType is ArgumentType.SetOfValues) {
            replacementCode.addStatement("query.append(\'(\')")
                .beginControlFlow("for (int i = 0; i < ${argument.name}.length; i++)")
                .addStatement("if (i != 0) query.append(\", \")")

            if (argument.argumentType.comparable?.dataType == SqliteType.TEXT) {
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
    method.addCode("return new \$T(", SQLDELIGHT_STATEMENT)
        .addCode("query.toString(), ")
        .addCode("args.toArray(new String[args.size()]), ")
    if (tablesUsed.isEmpty()) {
      method.addCode("\$T.<String>emptySet()", COLLECTIONS_TYPE)
    } else if (tablesUsed.size == 1) {
      method.addCode("\$T.<String>singleton(\"${tablesUsed.first()}\")", COLLECTIONS_TYPE)
    } else {
      method.addCode(unmodifiableListOfTables())
    }
    return method.addStatement(")").build()
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
    val SQLDELIGHT_INSERT_STATEMENT = SQLDELIGHT_COMPILED_STATEMENT.nestedClass("Insert")
    val SQLDELIGHT_UPDATE_STATEMENT = SQLDELIGHT_COMPILED_STATEMENT.nestedClass("Update")
    val SQLDELIGHT_DELETE_STATEMENT = SQLDELIGHT_COMPILED_STATEMENT.nestedClass("Delete")
    val SQLDELIGHT_STATEMENT = ClassName.get("com.squareup.sqldelight", "SqlDelightStatement")
    val SQLITEDATABASE_TYPE = ClassName.get("android.database.sqlite", "SQLiteDatabase")
    val LIST_TYPE = ClassName.get(List::class.java)
    val ARRAYLIST_TYPE = ClassName.get(ArrayList::class.java)
    val STRINGBUILDER_TYPE = ClassName.get(StringBuilder::class.java)
    val LINKEDHASHSET_TYPE = ClassName.get(LinkedHashSet::class.java)
    val ARRAYS_TYPE = ClassName.get(Arrays::class.java)
    val COLLECTIONS_TYPE = ClassName.get(Collections::class.java)
    val STRING_TYPE = ClassName.get(String::class.java)
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
