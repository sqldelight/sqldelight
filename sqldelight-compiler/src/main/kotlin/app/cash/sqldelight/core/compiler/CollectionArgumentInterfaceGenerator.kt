/*
 * Copyright (C) 2026 Square, Inc.
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

import app.cash.sqldelight.core.compiler.model.BindableQuery
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.DATA
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

internal class CollectionArgumentInterfaceGenerator(
  private val argument: BindableQuery.Argument,
) {
  fun kotlinImplementationSpec(): TypeSpec {
    val className = checkNotNull(argument.collectionTypeName)
    val typeSpec = TypeSpec.classBuilder(className.simpleName)
      .addModifiers(DATA)
    val constructor = FunSpec.constructorBuilder()

    argument.collectionElementTypes.orEmpty().forEach { field ->
      val javaType = field.javaType
      val typeWithoutAnnotations = javaType.copy(annotations = emptyList())
      typeSpec.addProperty(
        PropertySpec.builder(field.name, typeWithoutAnnotations)
          .initializer(field.name)
          .addAnnotations(javaType.annotations)
          .build(),
      )
      constructor.addParameter(field.name, typeWithoutAnnotations)
    }

    return typeSpec
      .primaryConstructor(constructor.build())
      .build()
  }
}
