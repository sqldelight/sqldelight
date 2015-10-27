package com.alecstrong.sqlite.android.model;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

public class JavatypeConstraint<T> extends ColumnConstraint<T> {
  private final String javatype;

  public JavatypeConstraint(String javatype, T originatingElement) {
    super(originatingElement);
    // Trim surrounding quotes if there are any.
    this.javatype = javatype.startsWith("\'") && javatype.endsWith("\'")
        ? javatype.substring(1, javatype.length()-1)
        : javatype;
  }

  public TypeName getJavatype() {
    return ClassName.bestGuess(javatype);
  }
}
