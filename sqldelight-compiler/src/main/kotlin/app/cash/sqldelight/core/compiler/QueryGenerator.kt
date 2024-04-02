package app.cash.sqldelight.core.compiler

import app.cash.sqldelight.core.compiler.integration.javadocText
import app.cash.sqldelight.core.compiler.model.BindableQuery
import app.cash.sqldelight.core.compiler.model.NamedMutator
import app.cash.sqldelight.core.compiler.model.NamedQuery
import app.cash.sqldelight.core.lang.ASYNC_RESULT_TYPE
import app.cash.sqldelight.core.lang.DRIVER_NAME
import app.cash.sqldelight.core.lang.MAPPER_NAME
import app.cash.sqldelight.core.lang.PREPARED_STATEMENT_TYPE
import app.cash.sqldelight.core.lang.encodedJavaType
import app.cash.sqldelight.core.lang.preparedStatementBinder
import app.cash.sqldelight.core.lang.util.childOfType
import app.cash.sqldelight.core.lang.util.columnDefSource
import app.cash.sqldelight.core.lang.util.findChildrenOfType
import app.cash.sqldelight.core.lang.util.isArrayParameter
import app.cash.sqldelight.core.lang.util.range
import app.cash.sqldelight.core.lang.util.rawSqlText
import app.cash.sqldelight.core.lang.util.sqFile
import app.cash.sqldelight.core.psi.SqlDelightStmtClojureStmtList
import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.grammar.mixins.BindParameterMixin
import com.alecstrong.sql.psi.core.psi.SqlBinaryEqualityExpr
import com.alecstrong.sql.psi.core.psi.SqlBindExpr
import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.NameAllocator

abstract class QueryGenerator(
  private val query: BindableQuery,
) {
  protected val dialect = query.statement.sqFile().dialect
  protected val treatNullAsUnknownForEquality = query.statement.sqFile().treatNullAsUnknownForEquality
  protected val generateAsync = query.statement.sqFile().generateAsync

  /**
   * Creates the block of code that prepares [query] as a prepared statement and binds the
   * arguments to it. This code block does not make any use of class fields, and only populates a
   * single variable [STATEMENT_NAME]
   *
   * val numberIndexes = createArguments(count = number.size)
   * val statement = database.prepareStatement("""
   *     |SELECT *
   *     |FROM player
   *     |WHERE number IN $numberIndexes
   *     """.trimMargin(), SqlPreparedStatement.Type.SELECT, 1 + (number.size - 1))
   * number.forEachIndexed { index, number ->
   *     check(this is SqlCursorSubclass)
   *     statement.bindLong(index + 2, number)
   *     }
   */
  protected fun executeBlock(): CodeBlock {
    val result = CodeBlock.builder()

    if (query.statement is SqlDelightStmtClojureStmtList) {
      if (query is NamedQuery) {
        result
          .apply { if (generateAsync) beginControlFlow("return %T", ASYNC_RESULT_TYPE) }
          .beginControlFlow(if (generateAsync) "transactionWithResult" else "return transactionWithResult")
      } else {
        result.beginControlFlow("transaction")
      }
      val handledArrayArgs = mutableSetOf<BindableQuery.Argument>()
      query.statement.findChildrenOfType<SqlStmt>().forEachIndexed { index, statement ->
        val (block, additionalArrayArgs) = executeBlock(statement, handledArrayArgs, query.idForIndex(index))
        handledArrayArgs.addAll(additionalArrayArgs)
        result.add(block)
      }
      result.endControlFlow()
      if (generateAsync && query is NamedQuery) {
        result.endControlFlow()
      }
    } else {
      result.add(executeBlock(query.statement, emptySet(), query.id).first)
    }

    return result.build()
  }

  private fun executeBlock(
    statement: PsiElement,
    handledArrayArgs: Set<BindableQuery.Argument>,
    id: Int,
  ): Pair<CodeBlock, Set<BindableQuery.Argument>> {
    val dialectPreparedStatementType = if (generateAsync) dialect.asyncRuntimeTypes.preparedStatementType else dialect.runtimeTypes.preparedStatementType

    val result = CodeBlock.builder()

    val positionToArgument = mutableListOf<Triple<Int, BindableQuery.Argument, SqlBindExpr?>>()
    val seenArgs = mutableSetOf<BindableQuery.Argument>()
    val duplicateTypes = mutableSetOf<IntermediateType>()
    query.arguments.forEach { argument ->
      if (argument.bindArgs.isNotEmpty()) {
        argument.bindArgs
          .filter { PsiTreeUtil.isAncestor(statement, it, true) }
          .forEach { bindArg ->
            if (!seenArgs.add(argument)) {
              duplicateTypes.add(argument.type)
            }
            positionToArgument.add(Triple(bindArg.node.textRange.startOffset, argument, bindArg))
          }
      } else {
        positionToArgument.add(Triple(0, argument, null))
      }
    }

    val bindStatements = CodeBlock.builder()
    val replacements = mutableListOf<Pair<IntRange, String>>()
    val argumentCounts = mutableListOf<String>()

    var needsFreshStatement = false

    val seenArrayArguments = mutableSetOf<BindableQuery.Argument>()

    val argumentNameAllocator = NameAllocator().apply {
      query.arguments.forEach { newName(it.type.name) }
    }

    // A list of [SqlBindExpr] in order of appearance in the query.
    val orderedBindArgs = positionToArgument.sortedBy { it.first }

    // The number of non-array bindArg's we've encountered so far.
    var nonArrayBindArgsCount = 0

    // A list of arrays we've encountered so far.
    val precedingArrays = mutableListOf<String>()

    val extractedVariables = mutableMapOf<IntermediateType, String>()
    // extract the variable for duplicate types, so we don't encode twice
    for (type in duplicateTypes) {
      if (type.bindArg?.isArrayParameter() == true) continue
      val encodedJavaType = type.encodedJavaType() ?: continue
      val variableName = argumentNameAllocator.newName(type.name)
      extractedVariables[type] = variableName
      bindStatements.add("val %N = $encodedJavaType\n", variableName)
    }
    // For each argument in the sql
    orderedBindArgs.forEach { (_, argument, bindArg) ->
      val type = argument.type
      // Need to replace the single argument with a group of indexed arguments, calculated at
      // runtime from the list parameter:
      // val idIndexes = id.mapIndexed { index, _ -> "?${previousArray.size + index}" }.joinToString(prefix = "(", postfix = ")")
      val offset = (precedingArrays.map { "$it.size" } + "$nonArrayBindArgsCount")
        .joinToString(separator = " + ").replace(" + 0", "")
      if (bindArg?.isArrayParameter() == true) {
        needsFreshStatement = true

        if (!handledArrayArgs.contains(argument) && seenArrayArguments.add(argument)) {
          result.addStatement(
            """
            |val ${type.name}Indexes = createArguments(count = ${type.name}.size)
            """.trimMargin(),
          )
        }

        // Replace the single bind argument with the array of bind arguments:
        // WHERE id IN ${idIndexes}
        replacements.add(bindArg.range to "\$${type.name}Indexes")

        // Perform the necessary binds:
        // id.forEachIndex { index, parameter ->
        //   statement.bindLong(previousArray.size + index, parameter)
        // }
        val indexCalculator = CodeBlock.of(
          if (offset == "0") {
            "index"
          } else {
            "index + %L"
          },
          offset,
        )
        val elementName = argumentNameAllocator.newName(type.name)
        bindStatements.add(
          """
          |${type.name}.forEachIndexed { index, $elementName ->
          |  %L}
          |
          """.trimMargin(),
          type.copy(name = elementName).preparedStatementBinder(indexCalculator),
        )

        precedingArrays.add(type.name)
        argumentCounts.add("${type.name}.size")
      } else {
        val bindParameter = bindArg?.bindParameter as? BindParameterMixin
        if (bindParameter == null || bindParameter.text != "DEFAULT") {
          nonArrayBindArgsCount += 1

          if (!treatNullAsUnknownForEquality && type.javaType.isNullable) {
            val parent = bindArg?.parent
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

              val block = CodeBlock.of("if (${type.name} == null) \"$nullableEquality\" else \"${symbol.text}\"")
              replacements.add(symbol.range to "\${ $block }")
            }
          }

          // Binds each parameter to the statement:
          // statement.bindLong(0, id)
          bindStatements.add(type.preparedStatementBinder(CodeBlock.of(offset), extractedVariables[type]))

          // Replace the named argument with a non named/indexed argument.
          // This allows us to use the same algorithm for non Sqlite dialects
          // :name becomes ?
          if (bindParameter != null) {
            replacements.add(bindArg.range to bindParameter.replaceWith(generateAsync, index = nonArrayBindArgsCount))
          }
        }
      }
    }

    val optimisticLock = if (query is NamedMutator.Update) {
      val columnsUpdated =
        query.update.updateStmtSubsequentSetterList.mapNotNull { it.columnName } +
          query.update.columnNameList
      columnsUpdated.singleOrNull {
        it.columnDefSource()!!.columnType.node.getChildren(null).any { it.text == "LOCK" }
      }
    } else {
      null
    }

    // Adds the actual SqlPreparedStatement:
    // statement = database.prepareStatement("SELECT * FROM test")
    val isNamedQuery = query is NamedQuery &&
      (statement == query.statement || statement == query.statement.children.filterIsInstance<SqlStmt>().last())
    if (nonArrayBindArgsCount != 0) {
      argumentCounts.add(0, nonArrayBindArgsCount.toString())
    }
    val arguments = mutableListOf<Any>(
      statement.rawSqlText(replacements),
      argumentCounts.ifEmpty { listOf(0) }.joinToString(" + "),
    )

    var binder: String

    if (argumentCounts.isEmpty()) {
      binder = ""
    } else {
      val binderLambda = CodeBlock.builder()
        .add(" {\n")
        .indent()

      if (PREPARED_STATEMENT_TYPE != dialectPreparedStatementType) {
        binderLambda.add("check(this is %T)\n", dialectPreparedStatementType)
      }

      binderLambda.add(bindStatements.build())
        .unindent()
        .add("}")
      arguments.add(binderLambda.build())
      binder = "%L"
    }
    if (generateAsync) {
      val awaiter = awaiting()

      if (isNamedQuery) {
        awaiter?.let { (bind, arg) ->
          binder += bind
          arguments.add(arg)
        }
      } else {
        binder += "%L"
        arguments.add(".await()")
      }
    }

    // Extract value from the result of a grouped statement in async,
    // because the transaction is put in an QueryResult.AsyncValue block.
    if (generateAsync && isNamedQuery && query.statement is SqlDelightStmtClojureStmtList) {
      binder += "%L"
      arguments.add(".await()")
    }

    val statementId = if (needsFreshStatement) CodeBlock.of("null") else CodeBlock.of("%L", id)

    if (isNamedQuery) {
      val execute = if (query.statement is SqlDelightStmtClojureStmtList) {
        "$DRIVER_NAME.executeQuery"
      } else {
        "return $DRIVER_NAME.executeQuery"
      }
      result.addStatement(
        "$execute(%L, %P, $MAPPER_NAME, %L)$binder",
        statementId,
        *arguments.toTypedArray(),
      )
    } else if (optimisticLock != null) {
      result.addStatement(
        "val result = $DRIVER_NAME.execute(%L, %P, %L)$binder",
        statementId,
        *arguments.toTypedArray(),
      )
    } else {
      result.addStatement(
        "$DRIVER_NAME.execute(%L, %P, %L)$binder",
        statementId,
        *arguments.toTypedArray(),
      )
    }

    if (query is NamedMutator.Update && optimisticLock != null) {
      result.addStatement(
        """
        if (result%L == 0L) throw %T(%S)
        """.trimIndent(),
        if (generateAsync) "" else ".value",
        ClassName("app.cash.sqldelight.db", "OptimisticLockException"),
        "UPDATE on ${query.tablesAffected.single().name} failed because optimistic lock ${optimisticLock.name} did not match",
      )
    }

    return Pair(result.build(), seenArrayArguments)
  }

  private fun PsiElement.leftWhitspace(): String {
    return if (prevSibling is PsiWhiteSpace) "" else " "
  }

  private fun PsiElement.rightWhitespace(): String {
    return if (nextSibling is PsiWhiteSpace) "" else " "
  }

  protected fun addJavadoc(builder: FunSpec.Builder) {
    if (query.javadoc != null) {
      builder.addKdoc(javadocText(query.javadoc))
    }
  }

  protected open fun awaiting(): Pair<String, String>? = "%L" to ".await()"
}
