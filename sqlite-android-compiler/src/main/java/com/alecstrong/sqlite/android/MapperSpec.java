package com.alecstrong.sqlite.android;

import com.alecstrong.sqlite.android.model.Column;
import com.alecstrong.sqlite.android.model.Table;
import com.google.common.base.Joiner;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;

public final class MapperSpec {
  private static final String CREATOR_TYPE_NAME = "Creator";
  private static final String CREATOR_FIELD = "creator";
  private static final String CREATOR_METHOD_NAME = "create";
  private static final TypeName CURSOR_TYPE = ClassName.get("android.database", "Cursor");
  private static final String CURSOR_PARAM = "cursor";
  private static final String COLUMN_INDEX_PARAM = "columnIndex";
  private static final String MAP_FUNCTION = "map";
  private static final String DEFAULTS_PARAM = "defaults";

  public static MapperSpec builder(Table<?> table) {
    return new MapperSpec(table);
  }

  private final Table<?> table;
  private final TypeName creatorType;

  private MapperSpec(Table<?> table) {
    this.table = table;
    creatorType = ParameterizedTypeName.get(ClassName.get(table.getPackageName(),
            table.interfaceName() + "." + table.mapperName() + "." + CREATOR_TYPE_NAME),
        TypeVariableName.get("T"));
  }

  public TypeSpec build() {
    TypeSpec.Builder mapper = TypeSpec.classBuilder(table.mapperName())
        .addTypeVariable(TypeVariableName.get("T", table.interfaceType()))
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .addField(creatorType, CREATOR_FIELD, Modifier.PRIVATE, Modifier.FINAL);

    mapper.addType(creatorInterface());

    for (Column column : table.getColumns()) {
      if (!column.isHandledType()) {
        TypeName columnCreatorType = ClassName.get(table.getPackageName(),
            table.interfaceName() + "." + table.mapperName() + "." + column.creatorName());
        mapper.addType(mapperInterface(column))
            .addField(columnCreatorType, column.creatorField(), Modifier.PRIVATE, Modifier.FINAL);
      }
    }

    return mapper //
        .addMethod(constructor())
        .addMethod(table.isKeyValue() ? keyValueMapperMethod() : mapperMethod())
        .build();
  }

  private MethodSpec constructor() {
    MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PROTECTED)
        .addParameter(creatorType, CREATOR_FIELD)
        .addStatement("this.$L = $L", CREATOR_FIELD, CREATOR_FIELD);

    for (Column column : table.getColumns()) {
      if (!column.isHandledType()) {
        TypeName columnCreatorType = ClassName.get(table.getPackageName(),
            table.interfaceName() + "." + table.mapperName() + "." + column.creatorName());
        constructor.addParameter(columnCreatorType, column.creatorField())
            .addStatement("this.$L = $L", column.creatorField(), column.creatorField());
      }
    }

    return constructor.build();
  }

  private MethodSpec mapperMethod() {
    CodeBlock.Builder mapReturn = CodeBlock.builder()
        .add("return $L.create(\n", CREATOR_FIELD)
        .indent();

    for (Column column : table.getColumns()) {
      if (!column.equals(table.getColumns().get(0))) mapReturn.add(",\n");
      if (column.isHandledType()) {
        mapReturn.add(cursorGetter(column));
      } else {
        if (column.isNullable()) {
          mapReturn.add("$L.isNull($L.getColumnIndex($L)) ? null : ", CURSOR_PARAM, CURSOR_PARAM,
              column.fieldName());
        }
        mapReturn.add("$L.$L($L, $L.getColumnIndex($L))", column.creatorField(),
            CREATOR_METHOD_NAME, CURSOR_PARAM, CURSOR_PARAM, column.fieldName());
      }
    }

    return MethodSpec.methodBuilder(MAP_FUNCTION)
        .addModifiers(Modifier.PUBLIC)
        .returns(TypeVariableName.get("T"))
        .addParameter(CURSOR_TYPE, CURSOR_PARAM)
        .addCode(mapReturn.unindent().add(");").build())
        .build();
  }

  private MethodSpec keyValueMapperMethod() {
    CodeBlock.Builder codeBlock = CodeBlock.builder();
    for (Column column : table.getColumns()) {
      codeBlock.addStatement("$T $L = $L == null ? $L : $L.$L()", column.getJavaType(),
          column.methodName(), DEFAULTS_PARAM, defaultFor(column), DEFAULTS_PARAM,
          column.methodName());
    }
    codeBlock.beginControlFlow("try")
        .beginControlFlow("while ($L.moveToNext())", CURSOR_PARAM)
        .addStatement("String key = cursor.getString(cursor.getColumnIndexOrThrow($S))",
            SqliteCompiler.KEY_VALUE_KEY_COLUMN)
        .beginControlFlow("switch (key)");

    List<String> methodNames = new ArrayList<String>();
    for (Column column : table.getColumns()) {
      codeBlock.add("case $L:\n", column.fieldName())
          .indent()
          .add("$L = ", column.methodName());

      if (column.isHandledType()) {
        codeBlock.add(cursorGetter(column, "\"" + SqliteCompiler.KEY_VALUE_VALUE_COLUMN + "\""));
      } else {
        if (column.isNullable()) {
          codeBlock.add("$L.isNull($L.getColumnIndex($S)) ? null : ", CURSOR_PARAM, CURSOR_PARAM,
              SqliteCompiler.KEY_VALUE_VALUE_COLUMN);
        }
        codeBlock.add("$L.$L($L, $L.getColumnIndex($S))", column.creatorField(),
            CREATOR_METHOD_NAME, CURSOR_PARAM, CURSOR_PARAM, SqliteCompiler.KEY_VALUE_VALUE_COLUMN);
      }
      codeBlock.addStatement(";\nbreak")
          .unindent();

      methodNames.add(column.methodName());
    }

    codeBlock.endControlFlow()
        .endControlFlow()
        .addStatement("return $L.$L($L)", CREATOR_FIELD, CREATOR_METHOD_NAME,
            Joiner.on(",\n").join(methodNames))
        .nextControlFlow("finally")
        .addStatement("cursor.close()")
        .endControlFlow();

    return MethodSpec.methodBuilder("map")
        .addModifiers(Modifier.PUBLIC)
        .returns(TypeVariableName.get("T"))
        .addParameter(CURSOR_TYPE, CURSOR_PARAM)
        .addParameter(table.interfaceType(), DEFAULTS_PARAM)
        .addCode(codeBlock.build())
        .build();
  }

  private CodeBlock cursorGetter(Column<?> column) {
    return cursorGetter(column, column.fieldName());
  }

  private CodeBlock cursorGetter(Column<?> column, String columnName) {
    CodeBlock.Builder code = CodeBlock.builder();
    if (column.isNullable()) {
      code.add("$L.isNull($L.getColumnIndex($L)) ? null : ", CURSOR_PARAM, CURSOR_PARAM,
          columnName);
    }
    switch (column.type) {
      case ENUM:
        return code
            .add("$T.valueOf($L.getString($L.getColumnIndex($L)))", column.getJavaType(),
                CURSOR_PARAM, CURSOR_PARAM, columnName)
            .build();
      case INT:
        return code
            .add("$L.getInt($L.getColumnIndex($L))", CURSOR_PARAM, CURSOR_PARAM, columnName)
            .build();
      case LONG:
        return code
            .add("$L.getLong($L.getColumnIndex($L))", CURSOR_PARAM, CURSOR_PARAM, columnName)
            .build();
      case SHORT:
        return code
            .add("$L.getShort($L.getColumnIndex($L))", CURSOR_PARAM, CURSOR_PARAM, columnName)
            .build();
      case DOUBLE:
        return code
            .add("$L.getDouble($L.getColumnIndex($L))", CURSOR_PARAM, CURSOR_PARAM, columnName)
            .build();
      case FLOAT:
        return code
            .add("$L.getFloat($L.getColumnIndex($L))", CURSOR_PARAM, CURSOR_PARAM, columnName)
            .build();
      case BOOLEAN:
        return code
            .add("$L.getInt($L.getColumnIndex($L)) == 1", CURSOR_PARAM, CURSOR_PARAM, columnName)
            .build();
      case BLOB:
        return code
            .add("$L.getBlob($L.getColumnIndex($L))", CURSOR_PARAM, CURSOR_PARAM, columnName)
            .build();
      case STRING:
        return code
            .add("$L.getString($L.getColumnIndex($L))", CURSOR_PARAM, CURSOR_PARAM, columnName)
            .build();
      default:
        throw new SqlitePluginException(column.getOriginatingElement(),
            "Unknown cursor getter for type " + column.getJavaType());
    }
  }

  private TypeSpec mapperInterface(Column<?> column) {
    return TypeSpec.interfaceBuilder(column.creatorName())
        .addModifiers(Modifier.PROTECTED)
        .addMethod(MethodSpec.methodBuilder(CREATOR_METHOD_NAME)
            .returns(column.getJavaType())
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addParameter(CURSOR_TYPE, CURSOR_PARAM)
            .addParameter(TypeName.INT, COLUMN_INDEX_PARAM)
            .build())
        .build();
  }

  private TypeSpec creatorInterface() {
    MethodSpec.Builder create = MethodSpec.methodBuilder(CREATOR_METHOD_NAME)
        .returns(TypeVariableName.get("R"))
        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);

    for (Column column : table.getColumns()) {
      create.addParameter(column.getJavaType(), column.methodName());
    }

    return TypeSpec.interfaceBuilder(CREATOR_TYPE_NAME)
        .addTypeVariable(TypeVariableName.get("R", table.interfaceType()))
        .addModifiers(Modifier.PROTECTED)
        .addMethod(create.build())
        .build();
  }

  private String defaultFor(Column column) {
    if (column.isNullable()) return "null";
    switch (column.type) {
      case ENUM:
      case STRING:
      case CLASS:
      case BLOB:
        return "null";
      case INT:
      case SHORT:
      case LONG:
      case DOUBLE:
      case FLOAT:
        return "0";
      case BOOLEAN:
        return "false";
    }
    throw new SqlitePluginException(column.getOriginatingElement(), "Unknown type " + column.type);
  }
}
