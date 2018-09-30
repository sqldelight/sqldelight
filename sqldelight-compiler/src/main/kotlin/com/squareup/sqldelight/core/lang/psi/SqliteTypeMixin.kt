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

import com.alecstrong.sqlite.psi.core.psi.impl.SqliteTypeNameImpl
import com.intellij.lang.ASTNode
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.sqldelight.core.lang.IntermediateType
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.BLOB
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.INTEGER
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.REAL
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.TEXT

internal abstract class SqliteTypeMixin(
    node: ASTNode
) : SqliteTypeNameImpl(node),
    TypedColumn {
  override fun type() = when (text) {
    "TEXT" -> IntermediateType(TEXT)
    "BLOB" -> IntermediateType(BLOB)
    "INTEGER" -> IntermediateType(INTEGER)
    "REAL" -> IntermediateType(REAL)
    else -> throw AssertionError()
  }

  override fun adapter(): PropertySpec? = null
}
