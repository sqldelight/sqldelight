package com.squareup.sqldelight.core.lang.psi

import com.alecstrong.sqlite.psi.core.psi.SqliteIdentifier
import com.alecstrong.sqlite.psi.core.psi.SqliteTypes
import com.alecstrong.sqlite.psi.core.psi.impl.SqliteColumnDefImpl
import com.intellij.lang.ASTNode
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.sqldelight.core.lang.util.parentOfType
import com.squareup.sqldelight.core.psi.SqlDelightAnnotation
import com.squareup.sqldelight.core.psi.SqlDelightAnnotationValue
import com.squareup.sqldelight.core.psi.SqlDelightColumnDef
import com.squareup.sqldelight.core.psi.SqlDelightJavaType
import com.squareup.sqldelight.core.psi.SqlDelightJavaTypeName
import com.squareup.sqldelight.core.psi.SqlDelightParameterizedJavaType
import com.squareup.sqldelight.core.psi.SqlDelightSqlStmtList

internal abstract class ColumnDefMixin(
    node: ASTNode
) : SqliteColumnDefImpl(node),
    TypedColumn,
    SqlDelightColumnDef {
  override fun type(): TypeName {
    var type = javaTypeName?.type() ?: sqliteType()
    if (columnConstraintList.none { it.node.findChildByType(SqliteTypes.NULL) != null }) {
      type = type.asNullable()
    }
    if (annotationList.isNotEmpty()) {
      type = type.annotated(annotationList.map { it.spec() })
    }
    return type
  }

  override fun adapter(): PropertySpec? {
    javaTypeName?.let {
      val customType = it.parameterizedJavaType?.type() ?: return null
      return PropertySpec
          .builder(
              name = "${columnName.name}Adapter",
              type = ParameterizedTypeName.get(columnAdapterType, customType, sqliteType()),
              modifiers = KModifier.INTERNAL
          )
          .build()
    }
    return null
  }

  private fun sqliteType(): TypeName = when (sqliteType.text) {
    "TEXT" -> String::class.asClassName()
    "BLOB" -> ByteArray::class.asClassName()
    "INTEGER" -> LONG
    "REAL" -> FLOAT
    else -> throw AssertionError()
  }

  private fun SqlDelightJavaTypeName.type(): TypeName {
    return parameterizedJavaType?.type() ?: when (text) {
      "Boolean" -> BOOLEAN
      "Short" -> SHORT
      "Long" -> LONG
      "Float" -> FLOAT
      "Double" -> DOUBLE
      "String" -> String::class.asClassName()
      "ByteArray" -> ByteArray::class.asClassName()
      else -> throw AssertionError()
    }
  }

  private fun SqlDelightJavaType.type(): ClassName {
    parentOfType<SqlDelightSqlStmtList>().importStmtList.forEach { import ->
      val typePrefix = text.substringBefore('.')
      if (import.javaType.text.endsWith(typePrefix)) {
        return ClassName(import.javaType.text.substringBefore(".$typePrefix"), text)
      }
    }
    return ClassName.bestGuess(text)
  }

  private fun SqlDelightParameterizedJavaType.type(): TypeName {
    if (javaTypeNameList.isNotEmpty() || javaTypeName2List.isNotEmpty()) {
      var parameters = javaTypeNameList.map { it.type() }.toTypedArray()
      if (javaTypeList.size == 2) {
        // Hack to get around '>>' character.
        parameters += ParameterizedTypeName.get(
            javaTypeList[1].type(),
            *javaTypeName2List.map { it.javaType.type() }.toTypedArray()
        )
      }
      return ParameterizedTypeName.get(javaTypeList[0].type(), *parameters)
    }
    return javaTypeList[0].type()
  }

  private fun SqlDelightAnnotation.spec(): AnnotationSpec {
    val annotation = AnnotationSpec.builder(javaType.type())
    val identifiers = findChildrenByClass(SqliteIdentifier::class.java)
    if (identifiers.isEmpty() && annotationValueList.isNotEmpty()) {
      annotation.addMember("value", annotationValueList[0].value())
    }
    annotationValueList.zip(identifiers, { annotation_value, identifier ->
      annotation.addMember(identifier.text, annotation_value.value())
    })
    return annotation.build()
  }

  private fun SqlDelightAnnotationValue.value(): CodeBlock {
    javaTypeName?.let {
      return CodeBlock.of("%T.class", it.type())
    }
    if (annotationValueList.isNotEmpty()) {
      return CodeBlock.of("{${annotationValueList.map { it.value() }.joinToString(",")}}")
    }
    return CodeBlock.of(text)
  }

  companion object {
    private val columnAdapterType = ClassName("com.squareup.sqldelight", "ColumnAdapter")
  }
}