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

  private final String name;
  private final Type type;

  final List<ColumnConstraint> columnConstraints = new ArrayList<>();

  JavatypeConstraint<T> javatypeConstraint;

  public Column(String name, Type type, T originatingElement) {
    super(originatingElement);
    this.name = name;
    this.type = type;
  }

  public TypeName getJavaType() {
    if (javatypeConstraint != null) {
      return javatypeConstraint.getJavatype();
    }
    return type.defaultType;
  }

  public boolean isHandledType() {
    if (javatypeConstraint == null) return true;
    return javatypeConstraint.isHandledType();
  }

  public boolean isEnum() {
    if (javatypeConstraint == null) return false;
    return javatypeConstraint.isEnum;
  }

  public void addConstraint(ColumnConstraint<T> columnConstraint) {
    if (columnConstraint instanceof JavatypeConstraint) {
      javatypeConstraint = (JavatypeConstraint<T>) columnConstraint;
    } else {
      columnConstraints.add(columnConstraint);
    }
  }

  public String methodName() {
    return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name);
  }

  public String fieldName() {
    return name.toUpperCase();
  }

  public String columnName() {
    return name;
  }

  public String creatorName() {
    return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name) + "Creator";
  }

  public String creatorField() {
    return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name) + "Creator";
  }
}
