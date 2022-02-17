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
package app.cash.sqldelight.core.lang.psi

import app.cash.sqldelight.core.compiler.SqlDelightCompiler.allocateName
import app.cash.sqldelight.core.lang.IntermediateType
import app.cash.sqldelight.core.lang.util.parentOfType
import app.cash.sqldelight.core.psi.SqlDelightAnnotation
import app.cash.sqldelight.core.psi.SqlDelightAnnotationValue
import app.cash.sqldelight.core.psi.SqlDelightColumnType
import app.cash.sqldelight.core.psi.SqlDelightJavaType
import app.cash.sqldelight.core.psi.SqlDelightJavaTypeName
import app.cash.sqldelight.core.psi.SqlDelightParameterizedJavaType
import app.cash.sqldelight.core.psi.SqlDelightStmtList
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlIdentifier
import com.alecstrong.sql.psi.core.psi.SqlTypeName
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.alecstrong.sql.psi.core.psi.impl.SqlColumnTypeImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.tree.TokenSet
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.joinToCode

internal abstract class ColumnTypeMixin(
  node: ASTNode
) : SqlColumnTypeImpl(node),
  TypedColumn,
  SqlDelightColumnType {
  override fun type(): IntermediateType {
    val columnName = (parent as SqlColumnDef).columnName
    val columnConstraintList = (parent as SqlColumnDef).columnConstraintList

    var type = typeName.type().copy(column = (parent as SqlColumnDef), name = allocateName(columnName))
    javaTypeName?.type()?.let { type = type.copy(javaType = it) }
    if (columnConstraintList.none {
      (it.node.findChildByType(SqlTypes.NOT) != null && it.node.findChildByType(SqlTypes.NULL) != null) ||
        it.node.findChildByType(SqlTypes.PRIMARY) != null
    }
    ) {
      type = type.asNullable()
    }
    if (annotationList.isNotEmpty()) {
      type = type.copy(
        javaType = type.javaType
          .copy(annotations = type.javaType.annotations + annotationList.map { it.spec() })
      )
    }
    return type
  }

  override fun adapter(): PropertySpec? {
    val columnName = (parent as SqlColumnDef).columnName
    javaTypeName?.let {
      val customType = try {
        it.parameterizedJavaType.type()
      } catch (e: IllegalArgumentException) {
        // Found an invalid type.
        return null
      }
      return PropertySpec
        .builder(
          name = "${allocateName(columnName)}Adapter",
          type = columnAdapterType.parameterizedBy(customType, typeName.type().dialectType.javaType)
        )
        .build()
    }
    return null
  }

  private fun SqlDelightJavaTypeName.type(): TypeName? {
    return try {
      parameterizedJavaType.type()
    } catch (e: IllegalArgumentException) {
      // Found an invalid type.
      null
    }
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
      var parameters = javaTypeNameList.mapNotNull { it.type() }.toTypedArray()
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
    annotationValueList.zip(
      identifiers,
      { annotation_value, identifier ->
        annotation.addMember(
          CodeBlock.builder()
            .add("${identifier.text} = ")
            .add(annotation_value.value())
            .build()
        )
      }
    )
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
    val children = node.getChildren(TokenSet.create(SqlTypes.ID))
    children.filter { (it.text == "as" || it.text == "As") && it.prevVisibleSibling?.psi is SqlTypeName }
      .forEach {
        annotationHolder.createErrorAnnotation(it.psi, "Expected 'AS', got '${it.text}'")
      }
  }

  private val ASTNode.prevVisibleSibling: ASTNode?
    get() = generateSequence(treePrev) { it.treePrev }
      .firstOrNull { it.psi !is PsiWhiteSpace }

  companion object {
    private val columnAdapterType = ClassName("app.cash.sqldelight", "ColumnAdapter")

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
