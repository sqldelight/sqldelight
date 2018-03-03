package com.squareup.sqldelight.core.compiler

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.joinToCode
import com.squareup.sqldelight.core.compiler.model.NamedMutator
import com.squareup.sqldelight.core.lang.DATABASE_NAME
import com.squareup.sqldelight.core.lang.EXECUTE_METHOD

class MutatorQueryGenerator(private val query: NamedMutator) {
  /**
   * The public api to execute [query]
   */
  fun function(): FunSpec {
    return FunSpec.builder(query.name)
        .returns(LONG)
        .addParameters(query.arguments.map { ParameterSpec.builder(it.name, it.javaType).build() })
        .addStatement(
            "return ${query.name}.$EXECUTE_METHOD(%L)",
            query.arguments.map { CodeBlock.of(it.name) }.joinToCode(", ")
        )
        .build()
  }

  fun value(): PropertySpec {
    return PropertySpec.builder(query.name, ClassName("", query.name.capitalize()), PRIVATE)
        .delegate(CodeBlock.Builder()
            .add("lazy {\n")
            .indent()
            .add(
                "${query.name.capitalize()}($DATABASE_NAME.getConnection().prepareStatement(%S))\n",
                query.statement.text
            )
            .unindent()
            .add("}")
            .build())
        .build()
  }
}