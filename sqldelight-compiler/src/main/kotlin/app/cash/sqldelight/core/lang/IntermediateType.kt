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
package app.cash.sqldelight.core.lang

import app.cash.sqldelight.core.compiler.integration.adapterName
import app.cash.sqldelight.core.compiler.integration.adapterProperty
import app.cash.sqldelight.core.lang.psi.ColumnTypeMixin
import app.cash.sqldelight.core.lang.util.isArrayParameter
import app.cash.sqldelight.dialect.api.IntermediateType
import com.alecstrong.sql.psi.core.psi.Queryable
import com.intellij.psi.util.PsiTreeUtil
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asClassName

internal fun IntermediateType.argumentType() = if (bindArg?.isArrayParameter() == true) {
  Collection::class.asClassName().parameterizedBy(javaType)
} else {
  javaType
}

/**
 * @return A [CodeBlock] which binds this type to [columnIndex] on [STATEMENT_NAME].
 *
 * eg: statement.bindBytes(0, tableNameAdapter.columnNameAdapter.encode(column))
 */
internal fun IntermediateType.preparedStatementBinder(
  columnIndex: String,
  extractedVariable: String? = null
): CodeBlock {
  val codeBlock = extractedVariable?.let { CodeBlock.of(it) } ?: encodedJavaType()
  if (codeBlock != null) {
    return dialectType.prepareStatementBinder(columnIndex, codeBlock)
  }

  val name = if (javaType.isNullable) "it" else this.name
  val decodedType = CodeBlock.of(name)
  val encodedType = dialectType.encode(decodedType)

  return dialectType.prepareStatementBinder(
    columnIndex,
    when {
      decodedType == encodedType -> CodeBlock.of(this.name)
      javaType.isNullable -> encodedType.wrapInLet(this)
      else -> encodedType
    }
  )
}

internal fun IntermediateType.encodedJavaType(): CodeBlock? {
  val name = if (javaType.isNullable) "it" else this.name
  return (column?.columnType as ColumnTypeMixin?)?.adapter()?.let { adapter ->
    val parent = PsiTreeUtil.getParentOfType(column, Queryable::class.java)
    val adapterName = parent!!.tableExposed().adapterName
    val value = dialectType.encode(
      CodeBlock.of("$adapterName.%N.encode($name)", adapter)
    )
    if (javaType.isNullable) {
      value.wrapInLet(this)
    } else {
      value
    }
  }
}

private fun CodeBlock.wrapInLet(type: IntermediateType): CodeBlock {
  return CodeBlock.builder()
    .add("${type.name}?.let { ")
    .add(this)
    .add(" }")
    .build()
}

internal fun IntermediateType.cursorGetter(columnIndex: Int): CodeBlock {
  var cursorGetter = dialectType.cursorGetter(columnIndex, CURSOR_NAME)

  if (!javaType.isNullable) {
    cursorGetter = CodeBlock.of("$cursorGetter!!")
  }

  return (column?.columnType as ColumnTypeMixin?)?.adapter()?.let { adapter ->
    val adapterName =
      PsiTreeUtil.getParentOfType(column, Queryable::class.java)!!.tableExposed().adapterName
    if (javaType.isNullable) {
      CodeBlock.of(
        "%L?.let { $adapterName.%N.decode(%L) }", cursorGetter, adapter,
        dialectType.decode(CodeBlock.of("it"))
      )
    } else {
      CodeBlock.of("$adapterName.%N.decode(%L)", adapter, dialectType.decode(cursorGetter))
    }
  } ?: run {
    val encodedType = cursorGetter
    val decodedType = dialectType.decode(encodedType)

    if (javaType.isNullable && encodedType != decodedType) {
      CodeBlock.of("%L?.let { %L }", cursorGetter, dialectType.decode(CodeBlock.of("it")))
    } else {
      decodedType
    }
  }
}

internal fun IntermediateType.parentAdapter(): PropertySpec? {
  if ((column?.columnType as? ColumnTypeMixin)?.adapter() == null) return null

  return PsiTreeUtil.getParentOfType(column, Queryable::class.java)!!.tableExposed().adapterProperty()
}
