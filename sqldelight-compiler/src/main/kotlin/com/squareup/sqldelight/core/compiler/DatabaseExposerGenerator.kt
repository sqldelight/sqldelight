package com.squareup.sqldelight.core.compiler

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.lang.DATABASE_SCHEMA_TYPE

internal class DatabaseExposerGenerator(
  val implementation: TypeSpec,
  val fileIndex: SqlDelightFileIndex
) {
  fun exposerObject(): TypeSpec {
    return TypeSpec.objectBuilder("${implementation.name}Exposer")
        .addModifiers(KModifier.INTERNAL)
        .addFunction(implementation.primaryConstructor!!.let { constructor ->
          FunSpec.builder("newInstance")
              .returns(ClassName(fileIndex.packageName, fileIndex.className))
              .addParameters(constructor.parameters)
              .addCode(CodeBlock.builder()
                  .add("return %N", implementation)
                  .add(constructor.parameters.map { CodeBlock.of("%N", it) }.joinToCode(", ", prefix = "(", suffix = ")"))
                  .build())
              .build()
        })
        .addProperty(PropertySpec.builder("schema", DATABASE_SCHEMA_TYPE)
            .initializer("%N.${DATABASE_SCHEMA_TYPE.simpleName}", implementation)
            .build())
        .build()
  }
}