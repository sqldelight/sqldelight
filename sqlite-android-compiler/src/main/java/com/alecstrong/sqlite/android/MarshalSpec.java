package com.alecstrong.sqlite.android;

import com.alecstrong.sqlite.android.model.Column;
import com.alecstrong.sqlite.android.model.Table;
import com.alecstrong.sqlite.android.util.TypeUtils;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;

public class MarshalSpec {
  private static final TypeName CONTENTVALUES_TYPE =
      ClassName.get("android.content", "ContentValues");
  private static final String CONTENTVALUES_FIELD = "contentValues";
  private static final String CONTENTVALUES_METHOD = "asContentValues";
  private static final String MARSHAL_METHOD_NAME = "marshal";
  private static final String COLUMN_NAME_PARAM = "columnName";

  public static MarshalSpec builder(Table<?> table) {
    return new MarshalSpec(table);
  }

  private final Table<?> table;

  public MarshalSpec(Table<?> table) {
    this.table = table;
  }

  TypeSpec build() {
    TypeSpec.Builder marshal = TypeSpec.classBuilder(table.marshalName())
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addField(FieldSpec.builder(CONTENTVALUES_TYPE, CONTENTVALUES_FIELD, Modifier.PROTECTED)
            .initializer("new $T()", CONTENTVALUES_TYPE)
            .build())
        .addMethod(MethodSpec.methodBuilder(CONTENTVALUES_METHOD)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .returns(CONTENTVALUES_TYPE)
            .addStatement("return $L", CONTENTVALUES_FIELD)
            .build());

    MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PROTECTED);

    for (Column column : table.getColumns()) {
      if (column.isHandledType()) {
        marshal.addMethod(marshalMethod(column));
      } else {
        TypeName columnMarshalType = ClassName.get(table.getPackageName(),
            table.interfaceName() + "." + table.marshalName() + "." + column.marshalName());
        marshal.addType(marshalInterface(column))
            .addField(columnMarshalType, column.marshalField(), Modifier.PRIVATE, Modifier.FINAL);
        constructor.addParameter(columnMarshalType, column.marshalField())
            .addStatement("this.$L = $L", column.marshalField(), column.marshalField());
        marshal.addMethod(MethodSpec.methodBuilder(column.methodName())
            .addModifiers(Modifier.PUBLIC)
            .returns(ClassName.get(table.getPackageName(),
                table.interfaceName() + "." + table.marshalName()))
            .addParameter(column.getJavaType(), column.methodName())
            .addStatement("$L.marshal($L, $L)", column.marshalField(), CONTENTVALUES_FIELD,
                column.fieldName())
            .addStatement("return this")
            .build());
      }
    }

    return marshal.addMethod(constructor.build())
        .build();
  }

  private TypeSpec marshalInterface(Column column) {
    return TypeSpec.interfaceBuilder(column.marshalName())
        .addModifiers(Modifier.PROTECTED)
        .addMethod(MethodSpec.methodBuilder(MARSHAL_METHOD_NAME)
            .addParameter(CONTENTVALUES_TYPE, CONTENTVALUES_FIELD)
            .addParameter(ClassName.get(String.class), COLUMN_NAME_PARAM)
            .returns(TypeName.VOID)
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .build())
        .build();
  }

  private MethodSpec marshalMethod(Column column) {
    MethodSpec.Builder method = MethodSpec.methodBuilder(column.methodName())
        .addModifiers(Modifier.PUBLIC)
        .addParameter(column.getJavaType(), column.methodName())
        .returns(ClassName.get(table.getPackageName(),
            table.interfaceName() + "." + table.marshalName()));
    switch (TypeUtils.getType(column)) {
      case INT:
      case LONG:
      case SHORT:
      case DOUBLE:
      case FLOAT:
      case STRING:
      case BLOB:
        method.addStatement("$L.put($L, $L)", CONTENTVALUES_FIELD, column.fieldName(),
            column.methodName());
        break;
      case BOOLEAN:
        method.addStatement("$L.put($L, $L ? 1 : 0)", CONTENTVALUES_FIELD, column.fieldName(),
            column.methodName());
        break;
      case ENUM:
        method.addStatement("$L.put($L, $L.name())", CONTENTVALUES_FIELD, column.fieldName(),
            column.methodName());
        break;
    }
    return method.addStatement("return this")
        .build();
  }
}
