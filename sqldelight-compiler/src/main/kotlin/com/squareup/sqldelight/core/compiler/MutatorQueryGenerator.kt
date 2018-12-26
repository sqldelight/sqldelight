package com.squareup.sqldelight.core.compiler

import com.alecstrong.sqlite.psi.core.psi.SqliteTypes
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.INNER
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode
import com.squareup.sqldelight.core.compiler.model.NamedMutator
import com.squareup.sqldelight.core.compiler.model.NamedQuery
import com.squareup.sqldelight.core.lang.DATABASE_NAME
import com.squareup.sqldelight.core.lang.EXECUTE_METHOD
import com.squareup.sqldelight.core.lang.IntermediateType
import com.squareup.sqldelight.core.lang.STATEMENT_NAME
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.psi.InsertStmtMixin
import com.squareup.sqldelight.core.lang.util.childOfType
import com.squareup.sqldelight.core.lang.util.isArrayParameter
import com.squareup.sqldelight.core.lang.util.rawSqlText
import com.squareup.sqldelight.core.lang.util.referencedTables
import com.squareup.sqldelight.core.lang.util.sqFile

class MutatorQueryGenerator(
  private val query: NamedMutator
) : QueryGenerator(query) {
  /**
   * The public api to execute [query]
   */
  fun function(): FunSpec {
    val function = FunSpec.builder(query.name)
        .also(this::addJavadoc)
        .addParameters(query.parameters.map {
          ParameterSpec.builder(it.name, it.argumentType()).build()
        })
    var arguments: List<IntermediateType> = query.arguments.map { it.type }
    if (arguments.any { it.bindArg?.isArrayParameter() == true }) {
      // We cant use a prepared statement field since the number of parameters can change.
      return function
          .addCode(preparedStatementBinder())
          .notifyQueries()
          .addStatement("$STATEMENT_NAME.execute()")
          .build()
    }
    if (query.statement is InsertStmtMixin && query.statement.acceptsTableInterface()) {
      arguments = arguments.map { it.copy(name = "${query.parameters.single().name}.${it.name}") }
    }
    return function
        .addStatement(
            "${query.name}.$EXECUTE_METHOD(%L)",
            arguments.map { CodeBlock.of(it.name) }.joinToCode(", ")
        )
        .build()
  }

  fun value(): PropertySpec {
    return PropertySpec.builder(query.name, ClassName("", query.name.capitalize()), PRIVATE)
        .initializer("${query.name.capitalize()}()")
        .build()
  }

  private fun FunSpec.Builder.notifyQueries() : FunSpec.Builder {
    val resultSetsUpdated = mutableListOf<NamedQuery>()
    query.statement.sqFile().iterateSqliteFiles { psiFile ->
      if (psiFile !is SqlDelightFile) return@iterateSqliteFiles true
      var tablesAffected = listOf(query.tableEffected)

      psiFile.triggers.forEach { trigger ->
        if (trigger.tableName?.name == query.tableEffected.name) {
          val triggered = when (query) {
            is NamedMutator.Delete -> trigger.childOfType(SqliteTypes.DELETE) != null
            is NamedMutator.Insert -> trigger.childOfType(SqliteTypes.INSERT) != null
            is NamedMutator.Update -> {
              val columns = trigger.columnNameList.map { it.name }
              val updateColumns = query.update.updateStmtSubsequentSetterList.map { it.columnName?.name } +
                  query.update.columnName?.name
              trigger.childOfType(SqliteTypes.UPDATE) != null && (columns.isEmpty() ||
                  updateColumns.any { it in columns })
            }
          }

          if (triggered) {
            // Also need to notify for the trigger.
            tablesAffected += trigger.insertStmtList.map {
              it.tableName.referencedTables().single()
            }
            tablesAffected += trigger.deleteStmtList.map {
              it.qualifiedTableName.tableName.referencedTables().single()
            }
            tablesAffected += trigger.updateStmtList.map {
              it.qualifiedTableName.tableName.referencedTables().single()
            }
          }
        }
      }

      resultSetsUpdated.addAll(psiFile.namedQueries
          .filter { query -> query.tablesObserved.any { it in tablesAffected } })

      return@iterateSqliteFiles true
    }

    if (resultSetsUpdated.isEmpty()) return this

    // The list of effected queries:
    // (queryWrapper.dataQueries.selectForId + queryWrapper.otherQueries.selectForId)
    // TODO: Only notify queries that were dirtied (check using dirtied method).
    addStatement("notifyQueries(%L)",
        resultSetsUpdated.map { it.queryProperty }.joinToCode(separator = " + "))

    return this
  }

  /**
   * The generated mutator type with a single execute() method which performs the query.
   *
   * eg:
   *   private inner class InsertColumns(private val statement: SqlPreparedStatement) {
   *     fun execute(id: Int): Long {
   *       statement.bindLong(0, id.toLong())
   *       val result = statement.execute()
   *       synchronized(queryWrapper.otherQueries.someSelect) {
   *         notifyQueries(queryWrapper.otherQueries.someSelect)
   *       }
   *       synchronized(queryWrapper.otherQueries.someOtherSelect) {
   *         notifyQueries(queryWrapper.otherQueries.someOtherSelect)
   *       }
   *       return result
   *     }
   *   }
   */
  fun type(): TypeSpec {
    // The type of the class:
    // private inner class InsertColumns
    val type = TypeSpec.classBuilder(query.name.capitalize())
        .addModifiers(INNER, PRIVATE)

    // The execute method:
    // fun execute(_id: Int): Long
    val executeMethod = FunSpec.builder(EXECUTE_METHOD)
        .addCode("val $STATEMENT_NAME = $DATABASE_NAME.getConnection().prepareStatement(${query.id}, ⇥%S⇤, %L, ${query.arguments.size})\n",
            query.statement.rawSqlText(), query.type())
        .apply {
          query.arguments.forEach { (index, parameter) ->
            addParameter(parameter.name, parameter.javaType)

            // statement binding code:
            // statement.bindLong(0, _id.toLong())
            addCode(parameter.preparedStatementBinder(index.toString()))
          }
        }
        .addStatement("$STATEMENT_NAME.execute()")
        .notifyQueries()
        .build()

    return type
        .addFunction(executeMethod)
        .build()
  }
}