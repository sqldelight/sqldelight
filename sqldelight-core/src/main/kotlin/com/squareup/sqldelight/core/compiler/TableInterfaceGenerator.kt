package com.squareup.sqldelight.core.compiler

import com.alecstrong.sqlite.psi.core.psi.SqliteCreateTableStmt
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.ABSTRACT
import com.squareup.kotlinpoet.KModifier.DATA
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PUBLIC
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.util.sqFile
import com.squareup.sqldelight.core.psi.SqlDelightColumnDef

class TableInterfaceGenerator(private val table: SqliteCreateTableStmt) {
  fun interfaceSpec(): TypeSpec {
    val typeSpec = TypeSpec.interfaceBuilder(table.tableName.name.capitalize())

    table.columnDefList.filterIsInstance<SqlDelightColumnDef>().forEach { column ->
      typeSpec.addFunction(FunSpec.builder(column.columnName.name)
          .addModifiers(PUBLIC, ABSTRACT)
          .returns(column.type())
          .build())
    }

    val adapters = table.columnDefList.filterIsInstance<SqlDelightColumnDef>()
        .mapNotNull { it.adapter() }

    if (adapters.isNotEmpty()) {
      typeSpec.addType(TypeSpec.classBuilder("Adapter")
          .primaryConstructor(FunSpec.constructorBuilder()
              .addParameters(adapters.map {
                ParameterSpec.builder(it.name, it.type, *it.modifiers.toTypedArray()).build()
              })
              .build())
          .addProperties(adapters.map {
            PropertySpec.builder(it.name, it.type, *it.modifiers.toTypedArray())
              .initializer(it.name)
              .build()
          })
          .addModifiers(DATA)
          .build())
    }

    typeSpec.addType(kotlinImplementationSpec())

    return typeSpec.build()
  }

  fun kotlinInterfaceSpec(): TypeSpec {
    val typeSpec = TypeSpec.interfaceBuilder("${table.tableName.name.capitalize()}Kt")
        .addSuperinterface(ClassName(table.sqFile().packageName, table.tableName.name.capitalize()))

    table.columnDefList.filterIsInstance<SqlDelightColumnDef>().forEach { column ->
      typeSpec.addFunction(FunSpec.builder(column.columnName.name)
          .addModifiers(OVERRIDE)
          .returns(column.type())
          .addStatement("return ${column.columnName.name}")
          .build())

      typeSpec.addProperty(column.columnName.name, column.type(), PUBLIC)
    }

    return typeSpec.build()
  }

  fun kotlinImplementationSpec(): TypeSpec {
    val typeSpec = TypeSpec.classBuilder("Impl")
        .addModifiers(DATA)
        .addSuperinterface(ClassName((table.containingFile as SqlDelightFile).packageName, "${table.tableName.name.capitalize()}Kt"))

    val constructor = FunSpec.constructorBuilder()

    table.columnDefList.filterIsInstance<SqlDelightColumnDef>().forEach { column ->
      typeSpec.addProperty(PropertySpec.builder(column.columnName.name, column.type(), OVERRIDE)
          .initializer(column.columnName.name)
          .build())
      constructor.addParameter(column.columnName.name, column.type(), OVERRIDE)
    }

    return typeSpec.primaryConstructor(constructor.build()).build()
  }
}