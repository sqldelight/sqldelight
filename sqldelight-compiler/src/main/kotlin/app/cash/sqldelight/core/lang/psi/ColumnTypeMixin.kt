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

import app.cash.sqldelight.core.capitalize
import app.cash.sqldelight.core.compiler.SqlDelightCompiler.allocateName
import app.cash.sqldelight.core.lang.types.typeResolver
import app.cash.sqldelight.core.lang.util.sqFile
import app.cash.sqldelight.core.psi.SqlDelightAnnotation
import app.cash.sqldelight.core.psi.SqlDelightAnnotationValue
import app.cash.sqldelight.core.psi.SqlDelightColumnType
import app.cash.sqldelight.core.psi.SqlDelightJavaType
import app.cash.sqldelight.core.psi.SqlDelightJavaTypeName
import app.cash.sqldelight.core.psi.SqlDelightParameterizedJavaType
import app.cash.sqldelight.core.psi.SqlDelightStmtList
import app.cash.sqldelight.dialect.api.DialectType
import app.cash.sqldelight.dialect.api.IntermediateType
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.Queryable
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.alecstrong.sql.psi.core.psi.SqlIdentifier
import com.alecstrong.sql.psi.core.psi.SqlTypeName
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.alecstrong.sql.psi.core.psi.impl.SqlColumnTypeImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import com.intellij.util.asSafely
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.jvm.jvmInline

internal abstract class ColumnTypeMixin(
  node: ASTNode,
) : SqlColumnTypeImpl(node),
  TypedColumn,
  SqlDelightColumnType {
  override fun type(): IntermediateType {
    val parent = parent as SqlColumnDef
    val columnName = parent.columnName
    val columnConstraintList = parent.columnConstraintList

    var type = typeResolver.definitionType(typeName).copy(column = parent, name = allocateName(columnName))
    javaTypeName?.type()?.let { type = type.copy(javaType = it) }

    // Ensure we use the same value type for a foreign key.
    val tableForeignKeyClause = (parent.parent as? SqlCreateTableStmt)?.tableConstraintList?.mapNotNull {
      val columnIndex = it.columnNameList.map { it.name }.indexOf(columnName.name)
      if (columnIndex != -1) {
        it.foreignKeyClause?.columnNameList?.get(columnIndex)
      } else {
        null
      }
    }?.singleOrNull()

    val columnConstraint = columnConstraintList.mapNotNull {
      it.foreignKeyClause
    }.singleOrNull() // Foreign Key
      ?.columnNameList?.singleOrNull() // Foreign Column

    (tableForeignKeyClause ?: columnConstraint)?.reference?.resolve()?.let { resolvedKey ->
      // Resolved Column
      val dialectType = resolvedKey.asSafely<SqlColumnName>() // Column Name
        ?.parent?.asSafely<SqlColumnDef>() // Column Definition
        ?.columnType?.asSafely<ColumnTypeMixin>() // Column type
        ?.type()?.dialectType?.asSafely<ValueTypeDialectType>() ?: return@let // SqlDelight type
      type = type.copy(
        dialectType = dialectType,
        javaType = dialectType.javaType,
      )
    }

    valueClass()?.let { valueType ->
      val newDialectType = ValueTypeDialectType(valueType.name!!, type.dialectType)
      type = type.copy(
        dialectType = newDialectType,
        javaType = newDialectType.javaType,
      )
    }
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
          .copy(annotations = type.javaType.annotations + annotationList.map { it.spec() }),
      )
    }
    return typeResolver.simplifyType(type)
  }

  override fun adapter(): PropertySpec? {
    if (type().simplified) return null
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
          type = columnAdapterType.parameterizedBy(customType, typeResolver.definitionType(typeName).dialectType.javaType),
        )
        .build()
    }
    return null
  }

  internal fun valueClass(): TypeSpec? {
    if (node.getChildren(null).all { it.text != "VALUE" && it.text != "LOCK" }) return null

    val columnName = (parent as SqlColumnDef).columnName.name
    val type = typeResolver.definitionType(typeName).javaType
    return TypeSpec.classBuilder(columnName.capitalize())
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addParameter(columnName, type)
          .build(),
      )
      .addProperty(
        PropertySpec.builder(columnName, type)
          .initializer(columnName)
          .build(),
      )
      .addModifiers(KModifier.VALUE)
      .jvmInline()
      .build()
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
    parentOfType<SqlDelightStmtList>()!!.importStmtList.importStmtList.forEach { import ->
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
            .build(),
        )
      },
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

  internal inner class ValueTypeDialectType(
    name: String,
    val wrappedType: DialectType,
  ) : DialectType by wrappedType {
    override val javaType: TypeName by lazy {
      val tableName = PsiTreeUtil.getParentOfType(this@ColumnTypeMixin, Queryable::class.java)!!.tableExposed().tableName
      ClassName(tableName.sqFile().packageName!!, allocateName(tableName).capitalize(), name)
    }

    override fun encode(value: CodeBlock): CodeBlock {
      val columnName = (parent as SqlColumnDef).columnName
      return wrappedType.encode(CodeBlock.of("%L.${columnName.text}", value))
    }

    override fun decode(value: CodeBlock) = CodeBlock.of("%T(%L)", javaType, wrappedType.decode(value))
  }

  private val ASTNode.prevVisibleSibling: ASTNode?
    get() = generateSequence(treePrev) { it.treePrev }
      .firstOrNull { it.psi !is PsiWhiteSpace }

  companion object {
    private val columnAdapterType = ClassName("app.cash.sqldelight", "ColumnAdapter")
  }
}
