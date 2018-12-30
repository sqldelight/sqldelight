package com.squareup.sqldelight.core.compiler

import com.intellij.openapi.module.Module
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.compiler.model.NamedExecute
import com.squareup.sqldelight.core.compiler.model.NamedMutator
import com.squareup.sqldelight.core.lang.DATABASE_NAME
import com.squareup.sqldelight.core.lang.DATABASE_TYPE
import com.squareup.sqldelight.core.lang.QUERY_WRAPPER_NAME
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.TRANSACTER_TYPE
import com.squareup.sqldelight.core.lang.queriesName

class QueriesTypeGenerator(
  module: Module,
  private val file: SqlDelightFile
) {
  private val queryWrapperType = ClassName(
      SqlDelightFileIndex.getInstance(module).packageName,
      "QueryWrapper"
  )

  /**
   * Generate the full queries object - done once per file, containing all labeled select and
   * mutator queries.
   *
   * eg: class DataQueries(
   *       private val queryWrapper: QueryWrapper,
   *       private val database: SqlDatabase,
   *       transactions: ThreadLocal<Transacter.Transaction>
   *     ) : Transacter(database, transactions)
   */
  fun generateType(): TypeSpec {
    val type = TypeSpec.classBuilder(file.queriesName.capitalize())
        .superclass(TRANSACTER_TYPE)

    val constructor = FunSpec.constructorBuilder()

    // Add the query wrapper as a constructor property:
    // private val queryWrapper: QueryWrapper
    type.addProperty(PropertySpec.builder(QUERY_WRAPPER_NAME, queryWrapperType, PRIVATE)
        .initializer(QUERY_WRAPPER_NAME)
        .build())
    constructor.addParameter(QUERY_WRAPPER_NAME, queryWrapperType)

    // Add the database as a constructor property and superclass parameter:
    // private val database: SqlDatabase
    type.addProperty(PropertySpec.builder(DATABASE_NAME, DATABASE_TYPE, PRIVATE)
        .initializer(DATABASE_NAME)
        .build())
    constructor.addParameter(DATABASE_NAME, DATABASE_TYPE)
    type.addSuperclassConstructorParameter(DATABASE_NAME)

    file.namedQueries.forEach { query ->
      tryWithElement(query.select) {
        val generator = SelectQueryGenerator(query)

        type.addProperty(generator.queryCollectionProperty())
        type.addFunction(generator.customResultTypeFunction())

        if (query.needsWrapper() && query.needsLambda()) {
          type.addFunction(generator.defaultResultTypeFunction())
        }

        if (query.arguments.isNotEmpty()) {
          type.addType(generator.querySubtype())
        }
      }
    }

    file.namedMutators.forEach { mutator ->
      type.addExecute(mutator)
    }

    file.namedExecutes.forEach { execute ->
      type.addExecute(execute)
    }

    return type.primaryConstructor(constructor.build())
        .build()
  }

  private fun TypeSpec.Builder.addExecute(execute: NamedExecute) {
    tryWithElement(execute.statement) {
      val generator = if (execute is NamedMutator) {
        MutatorQueryGenerator(execute)
      } else {
        ExecuteQueryGenerator(execute)
      }

      addFunction(generator.function())
    }
  }
}