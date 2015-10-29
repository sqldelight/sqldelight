package com.alecstrong.sqlite.android;

import com.alecstrong.sqlite.android.model.Column;
import com.alecstrong.sqlite.android.model.Table;
import com.alecstrong.sqlite.android.util.TypeUtils;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;

public class MapperSpec {
  private static final String CREATOR_TYPE_NAME = "Creator";
  private static final String CREATOR_FIELD = "creator";
  private static final String CREATOR_METHOD_NAME = "create";
  private static final TypeName CURSOR_TYPE = ClassName.get("android.database", "Cursor");
  private static final String CURSOR_PARAM = "cursor";
  private static final String COLUMN_INDEX_PARAM = "columnIndex";
  private static final String MAP_FUNCTION = "map";

  public static MapperSpec builder(Table<?> table) {
    return new MapperSpec(table);
  }

  private final Table<?> table;

  private MapperSpec(Table<?> table) {
    this.table = table;
  }

  public TypeSpec build() {
    ClassName creatorType = ClassName.get(table.getPackageName(),
        table.interfaceName() + "." + table.mapperName() + "." + CREATOR_TYPE_NAME);

    TypeSpec.Builder mapper = TypeSpec.classBuilder(table.mapperName())
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .addField(creatorType, CREATOR_FIELD, Modifier.PRIVATE, Modifier.FINAL);

    mapper.addType(creatorInterface());

    MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PROTECTED)
        .addParameter(creatorType, CREATOR_FIELD)
        .addStatement("this.$L = $L", CREATOR_FIELD, CREATOR_FIELD);

    CodeBlock.Builder mapReturn = CodeBlock.builder()
        .add("return $L.create(\n", CREATOR_FIELD)
        .indent();

    for (Column column : table.getColumns()) {
      if (!column.equals(table.getColumns().get(0))) mapReturn.add(",\n");
      if (column.isHandledType()) {
        mapReturn.add(cursorGetter(column));
      } else {
        TypeName columnCreatorType = ClassName.get(table.getPackageName(),
            table.interfaceName() + "." + table.mapperName() + "." + column.creatorName());
        mapper.addType(creatorInterface(column))
            .addField(columnCreatorType, column.creatorField(), Modifier.PRIVATE, Modifier.FINAL);
        constructor.addParameter(columnCreatorType, column.creatorField())
            .addStatement("this.$L = $L", column.creatorField(), column.creatorField());
        mapReturn.add("$L.$L($L, $L.getColumnIndex($L))", column.creatorField(),
            CREATOR_METHOD_NAME, CURSOR_PARAM, CURSOR_PARAM, column.fieldName());
      }
    }

    return mapper //
        .addMethod(constructor.build())
        .addMethod(MethodSpec.methodBuilder(MAP_FUNCTION)
            .returns(table.interfaceType())
            .addParameter(CURSOR_TYPE, CURSOR_PARAM)
            .addCode(mapReturn.unindent().add(");").build())
            .build())
        .build();
  }

  private CodeBlock cursorGetter(Column<?> column) {
    switch (TypeUtils.getType(column)) {
      case ENUM:
        return CodeBlock.builder()
            .add("$T.valueOf($L.getString($L.getColumnIndex($L)))", column.getJavaType(),
                CURSOR_PARAM, CURSOR_PARAM, column.fieldName())
            .build();
      case INT:
        return CodeBlock.builder()
            .add("$L.getInt($L.getColumnIndex($L))", CURSOR_PARAM, CURSOR_PARAM, column.fieldName())
            .build();
      case LONG:
        return CodeBlock.builder()
            .add("$L.getLong($L.getColumnIndex($L))", CURSOR_PARAM, CURSOR_PARAM,
                column.fieldName())
            .build();
      case SHORT:
        return CodeBlock.builder()
            .add("$L.getShort($L.getColumnIndex($L))", CURSOR_PARAM, CURSOR_PARAM,
                column.fieldName())
            .build();
      case DOUBLE:
        return CodeBlock.builder()
            .add("$L.getDouble($L.getColumnIndex($L))", CURSOR_PARAM, CURSOR_PARAM,
                column.fieldName())
            .build();
      case FLOAT:
        return CodeBlock.builder()
            .add("$L.getFloat($L.getColumnIndex($L))", CURSOR_PARAM, CURSOR_PARAM,
                column.fieldName())
            .build();
      case BOOLEAN:
        return CodeBlock.builder()
            .add("$L.getInt($L.getColumnIndex($L)) == 1", CURSOR_PARAM, CURSOR_PARAM,
                column.fieldName())
            .build();
      case BLOB:
        return CodeBlock.builder()
            .add("$L.getBlob($L.getColumnIndex($L))", CURSOR_PARAM, CURSOR_PARAM,
                column.fieldName())
            .build();
      case STRING:
        return CodeBlock.builder()
            .add("$L.getString($L.getColumnIndex($L))", CURSOR_PARAM, CURSOR_PARAM,
                column.fieldName())
            .build();
      default:
        throw new SqlitePluginException(column.getOriginatingElement(),
            "Unknown cursor getter for type " + column.getJavaType());
    }
  }

  private TypeSpec creatorInterface(Column<?> column) {
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
        .returns(table.interfaceType())
        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);

    table.getColumns().forEach( //
        column -> create.addParameter(column.getJavaType(), column.methodName()));

    return TypeSpec.interfaceBuilder(CREATOR_TYPE_NAME)
        .addModifiers(Modifier.PROTECTED)
        .addMethod(create.build())
        .build();
  }
}
