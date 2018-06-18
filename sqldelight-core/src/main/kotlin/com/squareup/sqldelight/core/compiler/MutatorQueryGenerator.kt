package com.squareup.sqldelight.core.compiler

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.INNER
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode
import com.squareup.sqldelight.core.compiler.model.NamedMutator
import com.squareup.sqldelight.core.compiler.model.NamedQuery
import com.squareup.sqldelight.core.lang.DATABASE_NAME
import com.squareup.sqldelight.core.lang.EXECUTE_METHOD
import com.squareup.sqldelight.core.lang.EXECUTE_RESULT
import com.squareup.sqldelight.core.lang.IntermediateType
import com.squareup.sqldelight.core.lang.STATEMENT_NAME
import com.squareup.sqldelight.core.lang.STATEMENT_TYPE
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.psi.InsertStmtMixin
import com.squareup.sqldelight.core.lang.util.isArrayParameter
import com.squareup.sqldelight.core.lang.util.rawSqlText
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
        .returns(LONG)
        .addParameters(query.parameters.map {
          ParameterSpec.builder(it.name, it.argumentType()).build()
        })
    var arguments: List<IntermediateType> = query.arguments.map { it.type }
    if (arguments.any { it.bindArg?.isArrayParameter() == true }) {
      // We cant use a prepared statement field since the number of parameters can change.
      return function
          .addCode(preparedStatementBinder())
          .notifyQueries()
          .addStatement("return $STATEMENT_NAME.execute()")
          .build()
    }
    if (query.statement is InsertStmtMixin && query.statement.acceptsTableInterface()) {
      arguments = arguments.map { it.copy(name = "${query.parameters.single().name}.${it.name}") }
    }
    return function
        .addStatement(
            "return ${query.name}.$EXECUTE_METHOD(%L)",
            arguments.map { CodeBlock.of(it.name) }.joinToCode(", ")
        )
        .build()
  }

  fun value(): PropertySpec {
    return PropertySpec.builder(query.name, ClassName("", query.name.capitalize()), PRIVATE)
        .delegate("""
          |lazy {
          |${query.name.capitalize()}($DATABASE_NAME.getConnection().prepareStatement(%S, %L, ${query.arguments.size}))
          |}""".trimMargin(), query.statement.rawSqlText(), query.type())
        .build()
  }

  private fun FunSpec.Builder.notifyQueries() : FunSpec.Builder {
    val resultSetsUpdated = mutableListOf<NamedQuery>()
    query.statement.sqFile().iterateSqliteFiles { psiFile ->
      if (psiFile !is SqlDelightFile) return@iterateSqliteFiles true
      resultSetsUpdated.addAll(psiFile.namedQueries
          .filter { it.tablesObserved.contains(query.tableEffected) })
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

    val constructor = FunSpec.constructorBuilder()

    // The statement constructor property:
    // (private val statement: SqlPreparedStatement)
    constructor.addParameter(STATEMENT_NAME, STATEMENT_TYPE)
    type.addProperty(PropertySpec.builder(STATEMENT_NAME, STATEMENT_TYPE, PRIVATE)
        .initializer(STATEMENT_NAME)
        .build())

    // The execute method:
    // fun execute(_id: Int): Long
    val executeMethod = FunSpec.builder(EXECUTE_METHOD)
        .returns(LONG)
        .apply {
          query.arguments.forEach { (index, parameter) ->
            addParameter(parameter.name, parameter.javaType)

            // statement binding code:
            // statement.bindLong(0, _id.toLong())
            addCode(parameter.preparedStatementBinder(index.toString()))
          }
        }
        .addStatement("val $EXECUTE_RESULT = $STATEMENT_NAME.execute()")
        .notifyQueries()
        .addStatement("return $EXECUTE_RESULT")
        .build()

    return type
        .primaryConstructor(constructor.build())
        .addFunction(executeMethod)
        .build()
  }
}