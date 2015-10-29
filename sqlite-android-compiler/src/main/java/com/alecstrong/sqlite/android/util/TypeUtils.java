package com.alecstrong.sqlite.android.util;

import com.alecstrong.sqlite.android.SqlitePluginException;
import com.alecstrong.sqlite.android.model.Column;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

public class TypeUtils {
  public enum Type {
    INT, LONG, SHORT, DOUBLE, FLOAT, BOOLEAN, STRING, BLOB, ENUM
  }

  public static Type getType(Column<?> column) {
    TypeName type = column.getJavaType();
    if (column.isEnum()) {
      return Type.ENUM;
    } else if (type == TypeName.INT || type == TypeName.INT.box()) {
      return Type.INT;
    } else if (type == TypeName.LONG || type == TypeName.LONG.box()) {
      return Type.LONG;
    } else if (type == TypeName.SHORT || type == TypeName.SHORT.box()) {
      return Type.SHORT;
    } else if (type == TypeName.DOUBLE || type == TypeName.DOUBLE.box()) {
      return Type.DOUBLE;
    } else if (type == TypeName.FLOAT || type == TypeName.FLOAT.box()) {
      return Type.FLOAT;
    } else if (type == TypeName.BOOLEAN || type == TypeName.BOOLEAN.box()) {
      return Type.BOOLEAN;
    } else if (type.equals(ArrayTypeName.of(TypeName.BYTE))) {
      return Type.BLOB;
    } else if (type.equals(ClassName.get("java.lang", "String"))) {
      return Type.STRING;
    }
    throw new SqlitePluginException(column.getOriginatingElement(),
        "Unknown type " + type);
  }
}
