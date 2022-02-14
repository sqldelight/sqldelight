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
package app.cash.sqldelight.core.compiler

import app.cash.sqldelight.core.compiler.model.NamedQuery
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.DATA
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

class QueryInterfaceGenerator(val query: NamedQuery) {
  fun kotlinImplementationSpec(): TypeSpec {
    val typeSpec = TypeSpec.classBuilder(query.name.capitalize())
      .addModifiers(DATA)

    val constructor = FunSpec.constructorBuilder()

    query.resultColumns.forEach {
      val javaType = it.javaType
      val typeWithoutAnnotations = javaType.copy(annotations = emptyList())
      typeSpec.addProperty(
        PropertySpec.builder(it.name, typeWithoutAnnotations)
          .initializer(it.name)
          .addAnnotations(javaType.annotations)
          .build()
      )
      constructor.addParameter(it.name, typeWithoutAnnotations)
    }

    return typeSpec
      .primaryConstructor(constructor.build())
      .build()
  }
}
