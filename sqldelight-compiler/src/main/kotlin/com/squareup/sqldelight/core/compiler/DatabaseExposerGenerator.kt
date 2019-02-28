package com.squareup.sqldelight.core.compiler

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.joinToCode
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.lang.DATABASE_SCHEMA_TYPE
import kotlin.reflect.KClass

internal class DatabaseExposerGenerator(
  val implementation: TypeSpec,
  val fileIndex: SqlDelightFileIndex
) {
  private val interfaceType = ClassName(fileIndex.packageName, fileIndex.className)

  fun exposedSchema(): PropertySpec {
    return PropertySpec.builder("schema", DATABASE_SCHEMA_TYPE)
        .addModifiers(KModifier.INTERNAL)
        .receiver(KClass::class.asTypeName().parameterizedBy(interfaceType))
        .getter(FunSpec.getterBuilder()
            .addStatement("return %N.Schema", implementation)
            .build())
        .build()
  }

  fun exposedConstructor(): FunSpec {
    return implementation.primaryConstructor!!.let { constructor ->
      FunSpec.builder("newInstance")
          .addModifiers(KModifier.INTERNAL)
          .returns(ClassName(fileIndex.packageName, fileIndex.className))
          .receiver(KClass::class.asTypeName().parameterizedBy(interfaceType))
          .addParameters(constructor.parameters)
          .addStatement("return %N(%L)", implementation, constructor.parameters.map { CodeBlock.of("%N", it) }.joinToCode(", "))
          .build()
    }
  }
}