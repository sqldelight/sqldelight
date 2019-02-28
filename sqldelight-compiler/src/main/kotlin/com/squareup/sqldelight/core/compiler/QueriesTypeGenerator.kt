package com.squareup.sqldelight.core.compiler

import com.intellij.openapi.module.Module
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.ABSTRACT
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.compiler.model.NamedExecute
import com.squareup.sqldelight.core.compiler.model.NamedMutator
import com.squareup.sqldelight.core.lang.CUSTOM_DATABASE_NAME
import com.squareup.sqldelight.core.lang.DRIVER_NAME
import com.squareup.sqldelight.core.lang.DRIVER_TYPE
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.TRANSACTER_IMPL_TYPE
import com.squareup.sqldelight.core.lang.TRANSACTER_TYPE
import com.squareup.sqldelight.core.lang.queriesImplType
import com.squareup.sqldelight.core.lang.queriesType

class QueriesTypeGenerator(
  private val module: Module,
  private val file: SqlDelightFile
) {

  fun interfaceType(): TypeSpec {
    val type = TypeSpec.interfaceBuilder(file.queriesType.simpleName)
        .addSuperinterface(TRANSACTER_TYPE)

    file.namedQueries.forEach { query ->
      tryWithElement(query.select) {
        val generator = SelectQueryGenerator(query)

        type.addFunction(generator.customResultTypeFunctionInterface()
            .addModifiers(ABSTRACT)
            .build())

        if (query.needsWrapper() && query.needsLambda()) {
          type.addFunction(generator.defaultResultTypeFunctionInterface()
              .addModifiers(ABSTRACT)
              .build())
        }
      }
    }

    file.namedMutators.forEach { mutator ->
      type.addExecute(mutator, true)
    }

    file.namedExecutes.forEach { execute ->
      type.addExecute(execute, true)
    }

    return type.build()
  }

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
    val type = TypeSpec.classBuilder(file.queriesImplType(file.packageName).simpleName)
        .addModifiers(PRIVATE)
        .superclass(TRANSACTER_IMPL_TYPE)
        .addSuperinterface(file.queriesType)

    val constructor = FunSpec.constructorBuilder()

    // Add the query wrapper as a constructor property:
    // private val queryWrapper: QueryWrapper
    val databaseType = ClassName(packageName, "${SqlDelightFileIndex.getInstance(module).className}Impl")

    type.addProperty(PropertySpec.builder(CUSTOM_DATABASE_NAME, databaseType, PRIVATE)
        .initializer(CUSTOM_DATABASE_NAME)
        .build())
    constructor.addParameter(CUSTOM_DATABASE_NAME, databaseType)

    // Add the database as a constructor property and superclass parameter:
    // private val driver: SqlDriver
    type.addProperty(PropertySpec.builder(DRIVER_NAME, DRIVER_TYPE, PRIVATE)
        .initializer(DRIVER_NAME)
        .build())
    constructor.addParameter(DRIVER_NAME, DRIVER_TYPE)
    type.addSuperclassConstructorParameter(DRIVER_NAME)

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
      type.addExecute(mutator, false)
    }

    file.namedExecutes.forEach { execute ->
      type.addExecute(execute, false)
    }

    return type.primaryConstructor(constructor.build())
        .build()
  }

  private fun TypeSpec.Builder.addExecute(execute: NamedExecute, forInterface: Boolean) {
    tryWithElement(execute.statement) {
      val generator = if (execute is NamedMutator) {
        MutatorQueryGenerator(execute)
      } else {
        ExecuteQueryGenerator(execute)
      }

      addFunction(
          if (forInterface) generator.interfaceFunction().addModifiers(ABSTRACT).build()
          else generator.function()
      )
    }
  }
}
