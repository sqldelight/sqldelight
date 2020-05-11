/*
 * Copyright (C) 2017 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.sqldelight.core.lang.psi

import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlIdentifier
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.alecstrong.sql.psi.core.psi.impl.SqlColumnDefImpl
import com.intellij.lang.ASTNode
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.joinToCode
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler.allocateName
import com.squareup.sqldelight.core.lang.IntermediateType
import com.squareup.sqldelight.core.lang.util.parentOfType
import com.squareup.sqldelight.core.psi.SqlDelightAnnotation
import com.squareup.sqldelight.core.psi.SqlDelightAnnotationValue
import com.squareup.sqldelight.core.psi.SqlDelightColumnDef
import com.squareup.sqldelight.core.psi.SqlDelightJavaType
import com.squareup.sqldelight.core.psi.SqlDelightJavaTypeName
import com.squareup.sqldelight.core.psi.SqlDelightParameterizedJavaType
import com.squareup.sqldelight.core.psi.SqlDelightStmtList

internal abstract class ColumnDefMixin(
  node: ASTNode
) : SqlColumnDefImpl(node),
    TypedColumn,
    SqlDelightColumnDef {
  override fun type(): IntermediateType {
    var type = typeName.type().copy(column = this, name = allocateName(columnName))
    javaTypeName?.type()?.let { type = type.copy(javaType = it) }
    if (columnConstraintList.none {
      it.node.findChildByType(SqlTypes.NULL) != null ||
          it.node.findChildByType(SqlTypes.PRIMARY) != null
    }) {
      type = type.asNullable()
    }
    if (annotationList.isNotEmpty()) {
      type = type.copy(javaType = type.javaType
          .copy(annotations = type.javaType.annotations + annotationList.map { it.spec() }))
    }
    return type
  }

  override fun adapter(): PropertySpec? {
    javaTypeName?.let {
      val customType = it.parameterizedJavaType?.type() ?: return null
      return PropertySpec
          .builder(
              name = "${allocateName(columnName)}Adapter",
              type = columnAdapterType.parameterizedBy(customType, typeName.type().sqliteType.javaType)
          )
          .build()
    }
    return null
  }

  private fun SqlDelightJavaTypeName.type(): TypeName? {
    return parameterizedJavaType?.type() ?: kotlinType(text)
  }

  private fun SqlDelightJavaType.type(): ClassName {
    parentOfType<SqlDelightStmtList>().importStmtList.importStmtList.forEach { import ->
      val typePrefix = text.substringBefore('.')
      if (import.javaType.text.endsWith(".$typePrefix")) {
        return text.split(".").drop(1).fold(import.javaType.type()) { current, nested ->
          current.nestedClass(nested)
        }
      }
    }
    return ClassName.bestGuess(text)
  }

  private fun SqlDelightParameterizedJavaType.type(): TypeName {
    if (javaTypeNameList.isNotEmpty() || javaTypeName2List.isNotEmpty()) {
      var parameters = javaTypeNameList.map { it.type() }.filterNotNull().toTypedArray()
      if (javaTypeList.size == 2) {
        // Hack to get around '>>' character.
        parameters += javaTypeList[1].type()
            .parameterizedBy(*javaTypeName2List.map { it.javaType.type() }.toTypedArray())
      }
      return javaTypeList[0].type().parameterizedBy(*parameters)
    }
    return javaTypeList[0].type()
  }

  private fun SqlDelightAnnotation.spec(): AnnotationSpec {
    val annotation = AnnotationSpec.builder(javaType.type())
    val identifiers = children.filterIsInstance<SqlIdentifier>()
    if (identifiers.isEmpty() && annotationValueList.isNotEmpty()) {
      annotation.addMember(annotationValueList[0].value())
    }
    annotationValueList.zip(identifiers, { annotation_value, identifier ->
      annotation.addMember(CodeBlock.builder()
          .add("${identifier.text} = ")
          .add(annotation_value.value())
          .build())
    })
    return annotation.build()
  }

  private fun SqlDelightAnnotationValue.value(): CodeBlock {
    javaTypeName?.let {
      return CodeBlock.of("%T::class", it.type())
    }
    annotation?.let {
      return CodeBlock.of("%L", it.spec())
    }
    if (annotationValueList.isNotEmpty()) {
      return annotationValueList.map { it.value() }.joinToCode(",", "[", "]")
    }
    return CodeBlock.of(text)
  }

  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    javaTypeName?.let { javaType ->
      if (javaType.type() == null) {
        annotationHolder.createErrorAnnotation(javaType, "Unknown type ${javaType.text}")
      }
    }
  }

  companion object {
    private val columnAdapterType = ClassName("com.squareup.sqldelight", "ColumnAdapter")

    internal fun kotlinType(text: String) = when (text) {
      "Integer", "Int" -> INT
      "Boolean" -> BOOLEAN
      "Short" -> SHORT
      "Long" -> LONG
      "Float" -> FLOAT
      "Double" -> DOUBLE
      "String" -> String::class.asClassName()
      "ByteArray" -> ByteArray::class.asClassName()
      else -> null
    }

    internal val TypeName.isArrayType get() = when (this) {
      is ParameterizedTypeName -> rawType == ARRAY
      BooleanArray::class.asClassName(),
      ByteArray::class.asClassName(),
      CharArray::class.asClassName(),
      DoubleArray::class.asClassName(),
      FloatArray::class.asClassName(),
      IntArray::class.asClassName(),
      LongArray::class.asClassName(),
      ShortArray::class.asClassName() -> true
      else -> false
    }
  }
}
