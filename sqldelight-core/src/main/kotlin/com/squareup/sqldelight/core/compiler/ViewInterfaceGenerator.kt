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

import com.alecstrong.sqlite.psi.core.psi.SqliteCreateViewStmt
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.DATA
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PUBLIC
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.sqldelight.core.lang.util.flatFunctions
import com.squareup.sqldelight.core.lang.util.sqFile

class ViewInterfaceGenerator(val view: SqliteCreateViewStmt) {
  fun interfaceSpec(): TypeSpec {
    return TypeSpec.interfaceBuilder(view.viewName.name.capitalize())
        .apply {
          view.compoundSelectStmt.queryExposed().flatFunctions().forEach {
            addFunction(it)
          }
        }
        .addType(kotlinImplementationSpec())
        .build()
  }

  private fun kotlinImplementationSpec(): TypeSpec {
    val typeSpec = TypeSpec.classBuilder("Impl")
        .addModifiers(DATA)
        .addSuperinterface(ClassName(view.sqFile().packageName, "${view.viewName.name.capitalize()}Kt"))

    val constructor = FunSpec.constructorBuilder()

    view.compoundSelectStmt.queryExposed().flatFunctions().forEach {
      typeSpec.addProperty(PropertySpec.builder(it.name, it.returnType!!, OVERRIDE)
          .initializer(it.name)
          .build())
      constructor.addParameter(it.name, it.returnType!!, OVERRIDE)
    }

    return typeSpec.primaryConstructor(constructor.build()).build()
  }

  fun kotlinInterfaceSpec(): TypeSpec {
    val typeSpec = TypeSpec.interfaceBuilder("${view.viewName.name.capitalize()}Kt")
        .addSuperinterface(ClassName(view.sqFile().packageName, view.viewName.name.capitalize()))

    view.compoundSelectStmt.queryExposed().flatFunctions().forEach {
      typeSpec.addFunction(FunSpec.builder(it.name)
          .addModifiers(OVERRIDE)
          .returns(it.returnType!!)
          .addStatement("return ${it.name}")
          .build())

      typeSpec.addProperty(it.name, it.returnType!!, PUBLIC)
    }

    return typeSpec.build()
  }
}
