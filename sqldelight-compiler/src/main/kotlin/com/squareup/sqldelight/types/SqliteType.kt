/*
 * Copyright (C) 2016 Square, Inc.
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
package com.squareup.sqldelight.types

import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName

internal enum class SqliteType(val defaultType: TypeName, val handledTypes: Set<TypeName>) {
  INTEGER(TypeName.LONG, setOf(
      TypeName.BOOLEAN, TypeName.BOOLEAN.box(), TypeName.SHORT, TypeName.SHORT.box(),
      TypeName.INT, TypeName.INT.box(), TypeName.LONG, TypeName.LONG.box())
  ),
  REAL(TypeName.DOUBLE, setOf(
      TypeName.FLOAT, TypeName.FLOAT.box(), TypeName.DOUBLE, TypeName.DOUBLE.box())
  ),
  TEXT(ClassName.get(String::class.java), setOf(ClassName.get(String::class.java))),
  BLOB(ArrayTypeName.of(TypeName.BYTE), setOf(ArrayTypeName.of(TypeName.BYTE))),
  NULL(TypeName.VOID.box(), setOf(TypeName.VOID.box()));

  fun contains(javaType: TypeName) = handledTypes.contains(javaType)
}
