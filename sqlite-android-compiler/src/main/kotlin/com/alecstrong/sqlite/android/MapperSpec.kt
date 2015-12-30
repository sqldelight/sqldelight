package com.alecstrong.sqlite.android

import com.alecstrong.sqlite.android.model.Column
import com.alecstrong.sqlite.android.model.Table
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import java.util.ArrayList
import javax.lang.model.element.Modifier

import com.google.common.base.CaseFormat.LOWER_CAMEL
import com.google.common.base.CaseFormat.LOWER_UNDERSCORE
import com.google.common.base.CaseFormat.UPPER_CAMEL

class MapperSpec private constructor(private val table: Table<*>) {
  private val creatorType = ParameterizedTypeName.get(ClassName.get(table.packageName,
      "${table.interfaceName}.${table.mapperName()}.$CREATOR_TYPE_NAME"),
      TypeVariableName.get("T"))

  fun build(): TypeSpec {
    val mapper = TypeSpec.classBuilder(table.mapperName())
        .addTypeVariable(TypeVariableName.get("T", table.interfaceType))
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .addField(creatorType, CREATOR_FIELD, Modifier.PRIVATE, Modifier.FINAL)

    mapper.addType(creatorInterface())

    for (column in table.columns) {
      if (column.isHandledType) continue;
      val columnCreatorType = ClassName.get(table.packageName,
          "${table.interfaceName}.${table.mapperName()}.${column.creatorName()}")
      mapper.addType(mapperInterface(column))
          .addField(columnCreatorType, column.creatorField(), Modifier.PRIVATE, Modifier.FINAL)
    }

    return mapper
        .addMethod(constructor())
        .addMethod(if (table.isKeyValue) keyValueMapperMethod() else mapperMethod())
        .build()
  }

  private fun constructor(): MethodSpec {
    val constructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PROTECTED)
        .addParameter(creatorType, CREATOR_FIELD)
        .addStatement("this.$CREATOR_FIELD = $CREATOR_FIELD")

    for (column in table.columns) {
      if (!column.isHandledType) {
        val columnCreatorType = ClassName.get(table.packageName,
            "${table.interfaceName}.${table.mapperName()}.${column.creatorName()}")
        constructor.addParameter(columnCreatorType, column.creatorField())
            .addStatement("this.${column.creatorField()} = ${column.creatorField()}")
      }
    }

    return constructor.build()
  }

  private fun mapperMethod(): MethodSpec {
    val mapReturn = CodeBlock.builder().add("$[return $CREATOR_FIELD.create(\n")

    for (column in table.columns) {
      if (column != table.columns[0]) mapReturn.add(",\n")
      if (column.isHandledType) {
        mapReturn.add(cursorMapper(column))
      } else {
        if (column.isNullable) {
          mapReturn.add("$CURSOR_PARAM.isNull(" +
              "$CURSOR_PARAM.getColumnIndex(${column.fieldName})) ? null : ")
        }
        mapReturn.add("${column.creatorField()}.$CREATOR_METHOD_NAME(" +
            "$CURSOR_PARAM, $CURSOR_PARAM.getColumnIndex(${column.fieldName}))")
      }
    }

    return MethodSpec.methodBuilder(MAP_FUNCTION)
        .addModifiers(Modifier.PUBLIC)
        .returns(TypeVariableName.get("T"))
        .addParameter(CURSOR_TYPE, CURSOR_PARAM)
        .addCode(mapReturn.add("$]\n);\n").build())
        .build()
  }

  private fun keyValueMapperMethod(): MethodSpec {
    val codeBlock = CodeBlock.builder()
    for (column in table.columns) {
      codeBlock.addStatement("${'$'}T ${column.methodName} = $DEFAULTS_PARAM == null " +
          "? ${column.defaultValue()} " +
          ": $DEFAULTS_PARAM.${column.methodName}()", column.javaType)
    }
    codeBlock.beginControlFlow("try")
        .beginControlFlow("while ($CURSOR_PARAM.moveToNext())")
        .addStatement("String key = cursor.getString(cursor.getColumnIndexOrThrow(${'$'}S))",
            SqliteCompiler.KEY_VALUE_KEY_COLUMN)
        .beginControlFlow("switch (key)")

    val methodNames = ArrayList<String>()
    for (column in table.columns) {
      codeBlock.add("case ${column.fieldName}:\n")
          .indent()
          .add("${column.methodName} = ")

      if (column.isHandledType) {
        codeBlock.add(cursorMapper(column, "\"${SqliteCompiler.KEY_VALUE_VALUE_COLUMN}\""))
      } else {
        if (column.isNullable) {
          codeBlock.add("$CURSOR_PARAM.isNull($CURSOR_PARAM.getColumnIndex(${'$'}S)) ? null : ",
              SqliteCompiler.KEY_VALUE_VALUE_COLUMN)
        }
        codeBlock.add("${column.creatorField()}.$CREATOR_METHOD_NAME($CURSOR_PARAM, " +
            "$CURSOR_PARAM.getColumnIndex(${'$'}S))", SqliteCompiler.KEY_VALUE_VALUE_COLUMN)
      }
      // Javapoet wants to put the break four spaces over, so we first have to unindent twice.
      codeBlock.unindent().unindent().addStatement(";\nbreak").indent()

      methodNames.add(column.methodName)
    }

    codeBlock.endControlFlow()
        .endControlFlow()
        .addStatement("return $CREATOR_FIELD.$CREATOR_METHOD_NAME(" +
            "${table.columns.map({ it.methodName }).joinToString(",\n")})")
        .nextControlFlow("finally")
        .addStatement("cursor.close()")
        .endControlFlow()

    return MethodSpec.methodBuilder("map")
        .addModifiers(Modifier.PUBLIC)
        .returns(TypeVariableName.get("T"))
        .addParameter(CURSOR_TYPE, CURSOR_PARAM)
        .addParameter(table.interfaceType, DEFAULTS_PARAM)
        .addCode(codeBlock.build())
        .build()
  }

  private fun cursorMapper(column: Column<*>, columnName: String = column.fieldName): CodeBlock {
    val code = CodeBlock.builder()
    if (column.isNullable) {
      code.add("$CURSOR_PARAM.isNull($CURSOR_PARAM.getColumnIndex($columnName)) ? null : ")
    }
    return code.add(column.cursorGetter("$CURSOR_PARAM.getColumnIndex($columnName)")).build()
  }

  private fun mapperInterface(column: Column<*>) =
      TypeSpec.interfaceBuilder(column.creatorName())
          .addModifiers(Modifier.PROTECTED)
          .addMethod(MethodSpec.methodBuilder(CREATOR_METHOD_NAME)
              .returns(column.javaType)
              .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
              .addParameter(CURSOR_TYPE, CURSOR_PARAM)
              .addParameter(TypeName.INT, COLUMN_INDEX_PARAM)
              .build())
          .build()

  private fun creatorInterface(): TypeSpec {
    val create = MethodSpec.methodBuilder(CREATOR_METHOD_NAME)
        .returns(TypeVariableName.get("R"))
        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)

    table.columns.forEach { create.addParameter(it.javaType, it.methodName) }

    return TypeSpec.interfaceBuilder(CREATOR_TYPE_NAME)
        .addTypeVariable(TypeVariableName.get("R", table.interfaceType))
        .addModifiers(Modifier.PROTECTED)
        .addMethod(create.build())
        .build()
  }

  private fun Table<*>.mapperName() = name + "Mapper"
  private fun Column<*>.creatorName() = LOWER_UNDERSCORE.to(UPPER_CAMEL, name) + "Mapper"
  private fun Column<*>.creatorField() = LOWER_UNDERSCORE.to(LOWER_CAMEL, name) + "Mapper"
  private fun Column<*>.defaultValue() =
      if (isNullable) "null"
      else when (type) {
        Column.Type.ENUM, Column.Type.STRING, Column.Type.CLASS, Column.Type.BLOB -> "null"
        Column.Type.INT, Column.Type.SHORT, Column.Type.LONG, Column.Type.DOUBLE, Column.Type.FLOAT -> "0"
        Column.Type.BOOLEAN -> "false"
        else -> throw SqlitePluginException(originatingElement as Any, "Unknown type " + type)
      }

  private fun Column<*>.cursorGetter(getter: String) =
      when (type) {
        Column.Type.ENUM -> CodeBlock.builder().add(
            "${'$'}T.valueOf($CURSOR_PARAM.getString($getter))", javaType).build()
        Column.Type.INT -> CodeBlock.builder().add("$CURSOR_PARAM.getInt($getter)").build()
        Column.Type.LONG -> CodeBlock.builder().add("$CURSOR_PARAM.getLong($getter)").build()
        Column.Type.SHORT -> CodeBlock.builder().add("$CURSOR_PARAM.getShort($getter)").build()
        Column.Type.DOUBLE -> CodeBlock.builder().add("$CURSOR_PARAM.getDouble($getter)").build()
        Column.Type.FLOAT -> CodeBlock.builder().add("$CURSOR_PARAM.getFloat($getter)").build()
        Column.Type.BOOLEAN -> CodeBlock.builder().add("$CURSOR_PARAM.getInt($getter) == 1").build()
        Column.Type.BLOB -> CodeBlock.builder().add("$CURSOR_PARAM.getBlob($getter)").build()
        Column.Type.STRING -> CodeBlock.builder().add("$CURSOR_PARAM.getString($getter)").build()
        else -> throw SqlitePluginException(originatingElement as Any,
            "Unknown cursor getter for type $javaType")
      }

  companion object {
    private val CREATOR_TYPE_NAME = "Creator"
    private val CREATOR_FIELD = "creator"
    private val CREATOR_METHOD_NAME = "create"
    private val CURSOR_TYPE = ClassName.get("android.database", "Cursor")
    private val CURSOR_PARAM = "cursor"
    private val COLUMN_INDEX_PARAM = "columnIndex"
    private val MAP_FUNCTION = "map"
    private val DEFAULTS_PARAM = "defaults"

    fun builder(table: Table<*>) = MapperSpec(table)
  }
}
