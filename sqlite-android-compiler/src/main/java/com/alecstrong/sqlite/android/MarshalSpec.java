package com.alecstrong.sqlite.android;

import com.alecstrong.sqlite.android.model.Column;
import com.alecstrong.sqlite.android.model.Table;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.lang.model.element.Modifier;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PROTECTED;

public final class MarshalSpec {
  private static final TypeName CONTENTVALUES_TYPE =
      ClassName.get("android.content", "ContentValues");
  private static final String CONTENTVALUES_FIELD = "contentValues";
  private static final String CONTENTVALUES_METHOD = "asContentValues";
  private static final String MARSHAL_METHOD_NAME = "marshal";
  private static final String COLUMN_NAME_PARAM = "columnName";
  private static final String CONTENTVALUES_MAP_FIELD = "contentValuesMap";
  private static final ParameterizedTypeName MAP_CLASS =
      ParameterizedTypeName.get(ClassName.get(Map.class), ClassName.get(String.class),
          CONTENTVALUES_TYPE);
  private static final String VALUE = "\"" + SqliteCompiler.KEY_VALUE_VALUE_COLUMN + "\"";

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
        .addTypeVariable(TypeVariableName.get("T", ClassName.get(table.getPackageName(),
            table.interfaceName() + "." + table.marshalName())))
        .addField(table.isKeyValue()
            ? FieldSpec.builder(MAP_CLASS, CONTENTVALUES_MAP_FIELD, FINAL, PROTECTED)
            .initializer("new $T<>()", ClassName.get(LinkedHashMap.class))
            .build()
            : FieldSpec.builder(CONTENTVALUES_TYPE, CONTENTVALUES_FIELD, PROTECTED)
                .initializer("new $T()", CONTENTVALUES_TYPE)
                .build())
        .addMethod(MethodSpec.methodBuilder(CONTENTVALUES_METHOD)
            .addModifiers(Modifier.PUBLIC, FINAL)
            .returns(table.isKeyValue()
                ? ParameterizedTypeName.get(ClassName.get(Collection.class), CONTENTVALUES_TYPE)
                : CONTENTVALUES_TYPE)
            .addStatement(table.isKeyValue() ? "return $L.values()" : "return $L",
                table.isKeyValue()
                    ? CONTENTVALUES_MAP_FIELD
                    : CONTENTVALUES_FIELD)
            .build());

    MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC);

    for (Column column : table.getColumns()) {
      if (column.isHandledType()) {
        marshal.addMethod(marshalMethod(column));
      } else {
        TypeName columnMarshalType = ClassName.get(table.getPackageName(),
            table.interfaceName() + "." + table.marshalName() + "." + column.marshalName());
        marshal.addType(marshalInterface(column))
            .addField(columnMarshalType, column.marshalField(), Modifier.PRIVATE, FINAL);
        constructor.addParameter(columnMarshalType, column.marshalField())
            .addStatement("this.$L = $L", column.marshalField(), column.marshalField());
        marshal.addMethod(contentValuesMethod(column)
            .addModifiers(Modifier.PUBLIC)
            .returns(TypeVariableName.get("T"))
            .addParameter(column.getJavaType(), column.methodName())
            .addStatement("$L.marshal($L, $L, $L)", column.marshalField(), CONTENTVALUES_FIELD,
                table.isKeyValue() ? VALUE : column.fieldName(), column.methodName())
            .addStatement("return (T) this")
            .build());
      }
    }

    return marshal.addMethod(constructor.build())
        .build();
  }

  private MethodSpec.Builder contentValuesMethod(Column column) {
    if (table.isKeyValue()) {
      MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(column.methodName());
      if (!column.isNullable() && !column.getJavaType().isPrimitive()) {
        methodBuilder.beginControlFlow("if ($L == null)", column.methodName())
            .addStatement(
                "throw new NullPointerException(\"Cannot insert NULL value for NOT NULL column $L\")",
                column.columnName())
            .endControlFlow();
      }
      return methodBuilder
          .addStatement("$T $L = $L.get($L)", CONTENTVALUES_TYPE, CONTENTVALUES_FIELD,
              CONTENTVALUES_MAP_FIELD, column.fieldName())
          .beginControlFlow("if ($L == null)", CONTENTVALUES_FIELD)
          .addStatement("$L = new $T()", CONTENTVALUES_FIELD, CONTENTVALUES_TYPE)
          .addStatement("$L.put($S, $L)", CONTENTVALUES_FIELD, SqliteCompiler.KEY_VALUE_KEY_COLUMN,
              column.fieldName())
          .addStatement("$L.put($L, $L)", CONTENTVALUES_MAP_FIELD, column.fieldName(),
              CONTENTVALUES_FIELD)
          .endControlFlow();
    }
    return MethodSpec.methodBuilder(column.methodName());
  }

  private TypeSpec marshalInterface(Column column) {
    return TypeSpec.interfaceBuilder(column.marshalName())
        .addModifiers(PROTECTED)
        .addMethod(MethodSpec.methodBuilder(MARSHAL_METHOD_NAME)
            .addParameter(CONTENTVALUES_TYPE, CONTENTVALUES_FIELD)
            .addParameter(ClassName.get(String.class), COLUMN_NAME_PARAM)
            .addParameter(column.getJavaType(), column.methodName())
            .returns(TypeName.VOID)
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .build())
        .build();
  }

  private MethodSpec marshalMethod(Column column) {
    MethodSpec.Builder method = contentValuesMethod(column)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(column.getJavaType(), column.methodName())
        .returns(TypeVariableName.get("T"));
    if (column.isNullable()) {
      method.beginControlFlow("if ($L == null)", column.methodName())
          .addStatement("$L.putNull($L)", CONTENTVALUES_FIELD,
              table.isKeyValue() ? VALUE : column.fieldName())
          .addStatement("return (T) this")
          .endControlFlow();
    }
    switch (column.type) {
      case INT:
      case LONG:
      case SHORT:
      case DOUBLE:
      case FLOAT:
      case STRING:
      case BLOB:
        method.addStatement("$L.put($L, $L)", CONTENTVALUES_FIELD,
            table.isKeyValue() ? VALUE : column.fieldName(), column.methodName());
        break;
      case BOOLEAN:
        method.addStatement("$L.put($L, $L ? 1 : 0)", CONTENTVALUES_FIELD,
            table.isKeyValue() ? VALUE : column.fieldName(), column.methodName());
        break;
      case ENUM:
        method.addStatement("$L.put($L, $L.name())", CONTENTVALUES_FIELD,
            table.isKeyValue() ? VALUE : column.fieldName(), column.methodName());
        break;
    }
    return method.addStatement("return (T) this")
        .build();
  }
}
