package com.alecstrong.sqlite.android.model;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.List;

public class Column<T> extends SqlElement<T> {
  public enum Type {
    INT(TypeName.INT, "INTEGER"),
    LONG(TypeName.LONG, "INTEGER"),
    SHORT(TypeName.SHORT, "INTEGER"),
    DOUBLE(TypeName.DOUBLE, "REAL"),
    FLOAT(TypeName.FLOAT, "REAL"),
    BOOLEAN(TypeName.BOOLEAN, "INTEGER"),
    STRING(ClassName.get(String.class), "TEXT"),
    BLOB(ArrayTypeName.of(TypeName.BYTE), "BLOB"),
    ENUM(null, "TEXT"),
    CLASS(null, "BLOB");

    public final String replacement;

    final TypeName defaultType;

    Type(TypeName defaultType, String replacement) {
      this.defaultType = defaultType;
      this.replacement = replacement;
    }
  }

  private final String name;
  private final Type type;
  private final TypeName classType;

  final List<ColumnConstraint> columnConstraints = new ArrayList<ColumnConstraint>();

  private NotNullConstraint<T> notNullConstraint;

  public Column(String name, Type type, T originatingElement) {
    super(originatingElement);
    this.name = name;
    this.type = type;
    this.classType = null;
  }

  public Column(String name, Type type, String fullyQualifiedClass, T originatingElement) {
    super(originatingElement);
    this.name = name;
    this.type = type;
    if (fullyQualifiedClass.startsWith("\'")) {
      // Strip quotes.
      fullyQualifiedClass = fullyQualifiedClass.substring(1, fullyQualifiedClass.length() - 1);
    }
    this.classType = ClassName.bestGuess(fullyQualifiedClass);
  }

  public TypeName getJavaType() {
    if (classType != null) {
      return classType;
    }
    if (notNullConstraint != null) {
      return type.defaultType;
    }
    return type.defaultType.box();
  }

  public boolean isHandledType() {
    return type != Type.CLASS;
  }

  public boolean isEnum() {
    return type == Type.ENUM;
  }

  public void addConstraint(ColumnConstraint<T> columnConstraint) {
    if (columnConstraint instanceof NotNullConstraint) {
      notNullConstraint = (NotNullConstraint<T>) columnConstraint;
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
    return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name) + "Mapper";
  }

  public String creatorField() {
    return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name) + "Mapper";
  }

  public String marshalName() {
    return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name) + "Marshal";
  }

  public String marshalField() {
    return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name) + "Marshal";
  }
}
