package app.cash.sqldelight.core.compiler

import app.cash.sqldelight.core.compiler.model.NamedExecute
import app.cash.sqldelight.core.compiler.model.NamedMutator
import app.cash.sqldelight.core.lang.ASYNC_TRANSACTER_IMPL_TYPE
import app.cash.sqldelight.core.lang.DRIVER_NAME
import app.cash.sqldelight.core.lang.SqlDelightQueriesFile
import app.cash.sqldelight.core.lang.TRANSACTER_IMPL_TYPE
import app.cash.sqldelight.core.lang.queriesType
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import com.intellij.openapi.module.Module
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

class QueriesTypeGenerator(
  private val module: Module,
  private val file: SqlDelightQueriesFile,
  private val dialect: SqlDelightDialect,
  private val generateAsync: Boolean,
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
    if (generateAsync) {
      check(dialect.asyncDriverType != null) { "Dialect $dialect does not support async drivers" }
    }
    val driverType = if (generateAsync) dialect.asyncDriverType!! else dialect.driverType

    val type = TypeSpec.classBuilder(file.queriesType.simpleName)
      .superclass(if (generateAsync) ASYNC_TRANSACTER_IMPL_TYPE else TRANSACTER_IMPL_TYPE)

    val constructor = FunSpec.constructorBuilder()

    // Add the driver as a constructor property and superclass parameter:
    // private val driver: SqlDriver
    type.addProperty(
      PropertySpec.builder(DRIVER_NAME, driverType, PRIVATE)
        .initializer(DRIVER_NAME)
        .build()
    )
    constructor.addParameter(DRIVER_NAME, driverType)
    type.addSuperclassConstructorParameter(DRIVER_NAME)

    // Add any required adapters.
    // private val tableAdapter: Table.Adapter
    file.requiredAdapters.forEach {
      type.addProperty(it)
      constructor.addParameter(it.name, it.type)
    }

    file.namedQueries.forEach { query ->
      tryWithElement(query.select) {
        val generator = SelectQueryGenerator(query, generateAsync)

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
        MutatorQueryGenerator(execute, generateAsync)
      } else {
        ExecuteQueryGenerator(execute, generateAsync)
      }

      addFunction(generator.function())
    }
  }
}
