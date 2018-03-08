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
import com.squareup.sqldelight.core.compiler.model.namedQueries
import com.squareup.sqldelight.core.lang.DATABASE_NAME
import com.squareup.sqldelight.core.lang.EXECUTE_METHOD
import com.squareup.sqldelight.core.lang.EXECUTE_RESULT
import com.squareup.sqldelight.core.lang.IntermediateType
import com.squareup.sqldelight.core.lang.STATEMENT_NAME
import com.squareup.sqldelight.core.lang.STATEMENT_TYPE
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.psi.InsertStmtMixin
import com.squareup.sqldelight.core.lang.util.rawSqlText
import com.squareup.sqldelight.core.lang.util.sqFile

class MutatorQueryGenerator(private val query: NamedMutator) {
  /**
   * The public api to execute [query]
   */
  fun function(): FunSpec {
    var arguments: List<IntermediateType> = query.arguments.map { it.second }
    if (query.statement is InsertStmtMixin && query.statement.acceptsTableInterface()) {
      arguments = arguments.map { it.copy(name = "${query.parameters.single().name}.${it.name}") }
    }
    return FunSpec.builder(query.name)
        .returns(LONG)
        .addParameters(query.parameters.map {
          ParameterSpec.builder(it.name, it.javaType).build()
        })
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
          |${query.name.capitalize()}($DATABASE_NAME.getConnection().prepareStatement(%S))
          |}""".trimMargin(), query.statement.rawSqlText())
        .build()
  }

  /**
   * The generated mutator type with a single execute() method which performs the query.
   *
   * eg:
   *   private inner class InsertColumns(private val statement: SqlPreparedStatement) {
   *     fun execute(id: Int): Long {
   *       statement.bindLong(0, id.toLong())
   *       val result = statement.execute()
   *       deferAction {
   *         (queryWrapper.otherQueries.someSelect + queryWrapper.otherQueries.someOtherSelect)
   *           .forEach { it.notifyResultSetChanged() }
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
        .apply {
          val resultSetsUpdated = mutableListOf<NamedQuery>()
          query.statement.sqFile().iterateSqliteFiles { psiFile ->
            if (psiFile !is SqlDelightFile) return@iterateSqliteFiles true
            resultSetsUpdated.addAll(psiFile.sqliteStatements().namedQueries()
                .filter { it.tablesObserved.contains(query.tableEffected) })
            return@iterateSqliteFiles true
          }

          if (resultSetsUpdated.isEmpty()) return@apply

          // If there are any result sets we update, defer an action to update them:
          // deferAction {
          //   (queryWrapper.dataQueries.selectForId + queryWrapper.otherQueries.selectForId)
          //     .forEach { it.notifyResultSetChanged() }
          // }
          // TODO: Only notify queries that were dirtied (check using dirtied method).
          addCode(CodeBlock.Builder()
              .add("deferAction {\n")
              .indent()
              .addStatement(
                  "%L.forEach { it.notifyResultSetChanged() }",
                  resultSetsUpdated.map { it.queryProperty }.joinToCode(" + ", "(", ")\n")
              )
              .unindent()
              .add("}\n")
              .build())
        }
        .addStatement("return $EXECUTE_RESULT")
        .build()

    return type
        .primaryConstructor(constructor.build())
        .addFunction(executeMethod)
        .build()
  }
}