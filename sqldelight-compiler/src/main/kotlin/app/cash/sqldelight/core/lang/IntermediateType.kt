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
import app.cash.sqldelight.core.dialect.api.DialectType
import app.cash.sqldelight.core.lang.psi.ColumnTypeMixin
import app.cash.sqldelight.core.lang.util.isArrayParameter
import com.alecstrong.sql.psi.core.psi.Queryable
import com.alecstrong.sql.psi.core.psi.SqlBindExpr
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.intellij.psi.util.PsiTreeUtil
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName

/**
 * Internal representation for a column type, which has SQLite data affinity as well as JVM class
 * type.
 */
internal data class IntermediateType(
  val dialectType: DialectType,
  val javaType: TypeName = dialectType.javaType,
  /**
   * The column definition this type is sourced from, or null if there is none.
   */
  val column: SqlColumnDef? = null,
  /**
   * The name of this intermediate type as exposed in the generated api.
   */
  val name: String = "value",
  /**
   * The original bind argument expression this intermediate type comes from.
   */
  val bindArg: SqlBindExpr? = null,
  /**
   * Whether or not this argument is extracted from a different type
   */
  val extracted: Boolean = false,
  /**
   * The types assumed to be compatible with this type. Validated at runtime.
   */
  val assumedCompatibleTypes: List<IntermediateType> = emptyList(),
) {
  fun asNullable() = copy(javaType = javaType.copy(nullable = true))

  fun asNonNullable() = copy(javaType = javaType.copy(nullable = false))

  fun nullableIf(predicate: Boolean): IntermediateType {
    return if (predicate) asNullable() else asNonNullable()
  }

  fun argumentType() = if (bindArg?.isArrayParameter() == true) {
    Collection::class.asClassName().parameterizedBy(javaType)
  } else {
    javaType
  }

  /**
   * @return A [CodeBlock] which binds this type to [columnIndex] on [STATEMENT_NAME].
   *
   * eg: statement.bindBytes(0, tableNameAdapter.columnNameAdapter.encode(column))
   */
  fun preparedStatementBinder(
    columnIndex: String,
    extractedVariable: String? = null
  ): CodeBlock {
    val codeBlock = extractedVariable?.let(CodeBlock::of) ?: encodedJavaType()
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
        javaType.isNullable -> encodedType.wrapInLet()
        else -> encodedType
      }
    )
  }

  fun encodedJavaType(): CodeBlock? {
    val name = if (javaType.isNullable) "it" else this.name
    return (column?.columnType as ColumnTypeMixin?)?.adapter()?.let { adapter ->
      val parent = PsiTreeUtil.getParentOfType(column, Queryable::class.java)
      val adapterName = parent!!.tableExposed().adapterName
      val value = dialectType.encode(
        CodeBlock.of("$adapterName.%N.encode($name)", adapter)
      )
      if (javaType.isNullable) {
        value.wrapInLet()
      } else {
        value
      }
    }
  }

  private fun CodeBlock.wrapInLet(): CodeBlock {
    return CodeBlock.builder()
      .add("${this@IntermediateType.name}?.let { ")
      .add(this)
      .add(" }")
      .build()
  }

  fun cursorGetter(columnIndex: Int): CodeBlock {
    var cursorGetter = dialectType.cursorGetter(columnIndex)

    if (!javaType.isNullable) {
      cursorGetter = CodeBlock.of("$cursorGetter!!")
    }

    return (column?.columnType as ColumnTypeMixin?)?.adapter()?.let { adapter ->
      val adapterName = PsiTreeUtil.getParentOfType(column, Queryable::class.java)!!.tableExposed().adapterName
      if (javaType.isNullable) {
        CodeBlock.of("%L?.let { $adapterName.%N.decode(%L) }", cursorGetter, adapter, dialectType.decode(CodeBlock.of("it")))
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
}
