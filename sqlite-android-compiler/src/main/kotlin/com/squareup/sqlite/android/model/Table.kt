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
package com.squareup.sqlite.android.model

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import com.squareup.sqlite.android.SqliteCompiler
import java.util.ArrayList

class Table<T>(val packageName: String, internal val name: String, val sqlTableName: String,
    originatingElement: T, val isKeyValue: Boolean) : SqlElement<T>(originatingElement) {
  val columns = ArrayList<Column<T>>()
  val interfaceName: String
    get() = SqliteCompiler.interfaceName(name)
  val interfaceType: TypeName
    get() = ClassName.get(packageName, interfaceName)
}
