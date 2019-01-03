package com.squareup.sqldelight.core.compiler

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.sqldelight.core.compiler.model.NamedExecute

open class ExecuteQueryGenerator(private val query: NamedExecute) : QueryGenerator(query) {
  protected open fun FunSpec.Builder.notifyQueries() = this

  /**
   * The public api to execute [query]
   */
  fun function(): FunSpec {
    return FunSpec.builder(query.name)
        .also(this::addJavadoc)
        .addParameters(query.parameters.map {
          ParameterSpec.builder(it.name, it.argumentType()).build()
        })
        .addCode(executeBlock())
        .notifyQueries()
        .build()
  }

  fun value(): PropertySpec {
    return PropertySpec.builder(query.name, ClassName("", query.name.capitalize()), KModifier.PRIVATE)
        .initializer("${query.name.capitalize()}()")
        .build()
  }
}