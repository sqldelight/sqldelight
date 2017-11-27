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

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.ABSTRACT
import com.squareup.kotlinpoet.KModifier.DATA
import com.squareup.kotlinpoet.KModifier.FINAL
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PUBLIC
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.sqldelight.core.lang.util.sqFile

class QueryInterfaceGenerator(val query: NamedQuery) {
  fun interfaceSpec(): TypeSpec {
    return TypeSpec.classBuilder("${query.name.capitalize()}Model")
        .addSuperinterface(ClassName(query.select.sqFile().packageName, query.name.capitalize()))
        .addModifiers(ABSTRACT)
        .apply {
          query.resultColumns.forEach {
            addFunction(FunSpec.builder(it.name)
                .addModifiers(ABSTRACT)
                .returns(it.javaType)
                .build())
            addProperty(PropertySpec.builder(it.name, it.javaType, OVERRIDE, FINAL)
                .getter(FunSpec.getterBuilder().addStatement("return ${it.name}()").build())
                .build())
          }
        }
        .build()
  }

  private fun kotlinImplementationSpec(): TypeSpec {
    val typeSpec = TypeSpec.classBuilder("Impl")
        .addModifiers(DATA)
        .addSuperinterface(ClassName(query.select.sqFile().packageName, query.name.capitalize()))

    val constructor = FunSpec.constructorBuilder()

    query.resultColumns.forEach {
      typeSpec.addProperty(PropertySpec.builder(it.name, it.javaType, OVERRIDE)
          .initializer(it.name)
          .build())
      constructor.addParameter(it.name, it.javaType, OVERRIDE)
    }

    return typeSpec.primaryConstructor(constructor.build()).build()
  }

  fun kotlinInterfaceSpec(): TypeSpec {
    val typeSpec = TypeSpec.interfaceBuilder(query.name.capitalize())

    query.resultColumns.forEach {
      typeSpec.addProperty(it.name, it.javaType, PUBLIC)
    }

    return typeSpec
        .addType(kotlinImplementationSpec())
        .build()
  }
}
