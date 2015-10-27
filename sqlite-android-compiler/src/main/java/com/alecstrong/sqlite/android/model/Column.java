package com.alecstrong.sqlite.android.model;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.List;

public class Column<T> extends SqlElement<T> {
  public enum Type {
    INTEGER(TypeName.LONG),
    REAL(TypeName.DOUBLE),
    TEXT(ClassName.get(String.class)),
    BLOB(ArrayTypeName.of(TypeName.BYTE));

    final TypeName defaultType;

    Type(TypeName defaultType) {
      this.defaultType = defaultType;
    }
  }

  public final String name;
  public final Type type;
  public final List<ColumnConstraint> columnConstraints = new ArrayList<>();

  public Column(String name, Type type, T originatingElement) {
    super(originatingElement);
    this.name = name;
    this.type = type;
  }

  public String getMethodName() {
    return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name);
  }

  public TypeName getJavaType() {
    for (ColumnConstraint columnConstraint : columnConstraints) {
      if (columnConstraint instanceof JavatypeConstraint) {
        return ((JavatypeConstraint) columnConstraint).getJavatype();
      }
    }
    return type.defaultType;
  }
}
