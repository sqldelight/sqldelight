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
package com.squareup.sqldelight.core.compiler

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.DATA
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.joinToCode
import com.squareup.sqldelight.core.compiler.model.NamedQuery
import com.squareup.sqldelight.core.lang.psi.ColumnTypeMixin.Companion.isArrayType

class QueryInterfaceGenerator(val query: NamedQuery) {
  fun kotlinImplementationSpec(): TypeSpec {
    val typeSpec = TypeSpec.classBuilder(query.name.capitalize())
        .addModifiers(DATA)

    val propertyPrints = mutableListOf<CodeBlock>()
    val contentToString = MemberName("kotlin.collections", "contentToString")

    val constructor = FunSpec.constructorBuilder()

    query.resultColumns.forEach {
      typeSpec.addProperty(PropertySpec.builder(it.name, it.javaType)
          .initializer(it.name)
          .build())
      constructor.addParameter(it.name, it.javaType)

      propertyPrints += if (it.javaType.isArrayType) {
        CodeBlock.of("${it.name}: \${${it.name}.%M()}", contentToString)
      } else {
        CodeBlock.of("${it.name}: \$${it.name}")
      }
    }

    typeSpec.addFunction(FunSpec.builder("toString")
        .returns(String::class.asClassName())
        .addModifiers(OVERRIDE)
        .addStatement("return %L", propertyPrints.joinToCode(
            separator = "\n|  ",
            prefix = "\"\"\"\n|${query.name.capitalize()} [\n|  ",
            suffix = "\n|]\n\"\"\".trimMargin()")
        )
        .build()
    )

    return typeSpec
        .primaryConstructor(constructor.build())
        .build()
  }
}
