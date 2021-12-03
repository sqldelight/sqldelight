package com.squareup.sqldelight.core.compiler

import com.intellij.openapi.module.Module
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.sqldelight.core.compiler.model.NamedExecute
import com.squareup.sqldelight.core.compiler.model.NamedMutator
import com.squareup.sqldelight.core.lang.DRIVER_NAME
import com.squareup.sqldelight.core.lang.DRIVER_TYPE
import com.squareup.sqldelight.core.lang.SqlDelightQueriesFile
import com.squareup.sqldelight.core.lang.TRANSACTER_IMPL_TYPE
import com.squareup.sqldelight.core.lang.queriesType

class QueriesTypeGenerator(
  private val module: Module,
  private val file: SqlDelightQueriesFile
) {
  /**
   * Generate the full queries object - done once per file, containing all labeled select and
   * mutator queries.
   *
   * eg: class DataQueries(
   *       private val queryWrapper: QueryWrapper,
   *       private val driver: SqlDriver,
   *       transactions: ThreadLocal<Transacter.Transaction>
   *     ) : TransacterImpl(driver, transactions)
   */
  fun generateType(packageName: String): TypeSpec {
    val type = TypeSpec.classBuilder(file.queriesType.simpleName)
      .superclass(TRANSACTER_IMPL_TYPE)

    val constructor = FunSpec.constructorBuilder()

    // Add the driver as a constructor property and superclass parameter:
    // private val driver: SqlDriver
    type.addProperty(
      PropertySpec.builder(DRIVER_NAME, DRIVER_TYPE, PRIVATE)
        .initializer(DRIVER_NAME)
        .build()
    )
    constructor.addParameter(DRIVER_NAME, DRIVER_TYPE)
    type.addSuperclassConstructorParameter(DRIVER_NAME)

    // Add any required adapters.
    // private val tableAdapter: Table.Adapter
    file.requiredAdapters.forEach {
      type.addProperty(it)
      constructor.addParameter(it.name, it.type)
    }

    file.namedQueries.forEach { query ->
      tryWithElement(query.select) {
        val generator = SelectQueryGenerator(query)

        type.addFunction(generator.customResultTypeFunction())

        if (query.needsWrapper()) {
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
