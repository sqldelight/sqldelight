package com.squareup.sqldelight.core.compiler

import com.alecstrong.sql.psi.core.psi.SqlBinaryEqualityExpr
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.alecstrong.sql.psi.core.sqlite_3_18.psi.BindParameter
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.sqldelight.core.compiler.model.BindableQuery
import com.squareup.sqldelight.core.compiler.model.NamedQuery
import com.squareup.sqldelight.core.lang.DRIVER_NAME
import com.squareup.sqldelight.core.lang.util.childOfType
import com.squareup.sqldelight.core.lang.util.isArrayParameter
import com.squareup.sqldelight.core.lang.util.range
import com.squareup.sqldelight.core.lang.util.rawSqlText

abstract class QueryGenerator(private val query: BindableQuery) {
  /**
   * Creates the block of code that prepares [query] as a prepared statement and binds the
   * arguments to it. This code block does not make any use of class fields, and only populates a
   * single variable [STATEMENT_NAME]
   *
   * val numberIndexes = createArguments(count = number.size, offset = 2)
   * val statement = database.prepareStatement("""
   *     |SELECT *
   *     |FROM player
   *     |WHERE number IN $numberIndexes
   *     """.trimMargin(), SqlPreparedStatement.Type.SELECT, 1 + (number.size - 1))
   * number.forEachIndexed { index, number ->
   *     statement.bindLong(index + 2, number)
   *     }
   */
  protected fun executeBlock(): CodeBlock {
    val result = CodeBlock.builder()

    val maxIndex = query.arguments.filter { it.type.bindArg?.isArrayParameter() != true }.map { it.index }.max() ?: 0
    val precedingArrays = mutableListOf<String>()
    val bindStatements = CodeBlock.builder()
    val replacements = mutableListOf<Pair<IntRange, String>>()
    val argumentCounts = mutableListOf<String>()

    var needsFreshStatement = false

    query.arguments.filterNot { it.type.bindArg?.isArrayParameter() == true }.size.let {
      if (it != 0) {
        argumentCounts.add(it.toString())
      }
    }

    // For each parameter in the sql
    query.arguments.forEach { (index, argument, args) ->
      if (argument.bindArg?.isArrayParameter() == true) {
        // For now, disable array parameters for non-sqlite
        if (argument.bindArg.bindParameter !is BindParameter) {
          throw IllegalStateException("Array parameters are not supported outside of SQLite.")
        }
        needsFreshStatement = true

        // Need to replace the single argument with a group of indexed arguments, calculated at
        // runtime from the list parameter:
        // val idIndexes = id.mapIndexed { index, _ -> "?${1 + previousArray.size + index}" }.joinToString(prefix = "(", postfix = ")")
        val offset = (precedingArrays.map { "$it.size" } + "${maxIndex + 1}")
          .joinToString(separator = " + ")
        val indexCalculator = "index + $offset"
        result.addStatement("""
          |val ${argument.name}Indexes = createArguments(count = ${argument.name}.size, offset = $offset)
        """.trimMargin())

        // Replace the single bind argument with the array of bind arguments:
        // WHERE id IN ${idIndexes}
        args.forEach {
          replacements.add(it.range to "\$${argument.name}Indexes")
        }

        // Perform the necessary binds:
        // id.forEachIndex { index, parameter ->
        //   statement.bindLong(1 + previousArray.size + index, parameter)
        // }
        bindStatements.addStatement("""
          |${argument.name}.forEachIndexed { index, ${argument.name} ->
          |%L}
        """.trimMargin(), argument.preparedStatementBinder(indexCalculator))

        precedingArrays.add(argument.name)
        argumentCounts.add("${argument.name}.size")
      } else {
        if (argument.javaType.isNullable) {
          val parent = argument.bindArg?.parent
          if (parent is SqlBinaryEqualityExpr) {
            needsFreshStatement = true

            var symbol = parent.childOfType(SqlTypes.EQ) ?: parent.childOfType(SqlTypes.EQ2)
            val nullableEquality: String
            if (symbol != null) {
              nullableEquality = "${symbol.leftWhitspace()}IS${symbol.rightWhitespace()}"
            } else {
              symbol = parent.childOfType(SqlTypes.NEQ) ?: parent.childOfType(SqlTypes.NEQ2)!!
              nullableEquality = "${symbol.leftWhitspace()}IS NOT${symbol.rightWhitespace()}"
            }

            val block = CodeBlock.of("if (${argument.name} == null) \"$nullableEquality\" else \"${symbol.text}\"")
            replacements.add(symbol.range to "\${ $block }")
          }
        }
        // Binds each parameter to the statement:
        // statement.bindLong(1, id)
        bindStatements.add(argument.preparedStatementBinder(index.toString()))

        // Replace the argument with an indexed argument. (Do this always to make generated code
        // less bug-prone):
        // :name becomes ?1
        args.forEach {
          if (it.bindParameter is BindParameter) {
            // We only do this for sqlite.
            replacements.add(it.range to "?$index")
          }
        }
      }
    }

    val id = if (needsFreshStatement) "null" else "${query.id}"

    // Adds the actual SqlPreparedStatement:
    // statement = database.prepareStatement("SELECT * FROM test")
    val executeMethod = if (query is NamedQuery) {
      "return $DRIVER_NAME.executeQuery"
    } else {
      "$DRIVER_NAME.execute"
    }
    val arguments = mutableListOf<Any>(
        query.statement.rawSqlText(replacements),
        argumentCounts.ifEmpty { listOf(0) }.joinToString(" + ")
    )
    val binder: String

    if (argumentCounts.isEmpty()) {
      binder = ""
    } else {
      arguments.add(CodeBlock.builder()
          .addStatement(" {")
          .indent()
          .add(bindStatements.build())
          .unindent()
          .add("}")
          .build())
      binder = "%L"
    }
    result.add("$executeMethod($id, %P, %L)$binder\n", *arguments.toTypedArray())

    return result.build()
  }

  private fun PsiElement.leftWhitspace(): String {
    return if (prevSibling is PsiWhiteSpace) "" else " "
  }

  private fun PsiElement.rightWhitespace(): String {
    return if (nextSibling is PsiWhiteSpace) "" else " "
  }

  protected fun addJavadoc(builder: FunSpec.Builder) {
    query.javadocText()?.let { builder.addKdoc(it) }
  }
}
