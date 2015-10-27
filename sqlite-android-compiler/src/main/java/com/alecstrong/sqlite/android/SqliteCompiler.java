package com.alecstrong.sqlite.android;

import com.alecstrong.sqlite.android.model.Column;
import com.alecstrong.sqlite.android.model.SqlStmt;
import com.alecstrong.sqlite.android.model.Table;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;

public class SqliteCompiler<T> {
  public Status<T> write(Table<T> table) {
    TypeSpec.Builder typeSpec = TypeSpec.interfaceBuilder(table.interfaceName())
        .addModifiers(Modifier.PUBLIC);

    for (Column<T> column : table.getColumns()) {
      typeSpec.addField(FieldSpec.builder(ClassName.get(String.class), column.name.toUpperCase())
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
          .initializer("$S", column.name)
          .build());

      try {
        typeSpec.addMethod(MethodSpec.methodBuilder(column.getMethodName())
            .returns(column.getJavaType())
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .build());
      } catch (Exception e) {
        return new Status<>(column.getOriginatingElement(), "Unknown type " + column.getJavaType(),
            Status.Result.FAILURE);
      }
    }

    for (SqlStmt<T> sqlStmt : table.getSqlStmts()) {
      typeSpec.addField(
          FieldSpec.builder(ClassName.get(String.class), sqlStmt.identifier.toUpperCase())
              .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
              .initializer("$S", sqlStmt.stmt)
              .build());
    }

    JavaFile javaFile = JavaFile.builder(table.getPackageName(), typeSpec.build()).build();

    try {
      javaFile.writeTo(table.getOutputDirectory());
      return new Status<>(table.getOriginatingElement(), "", Status.Result.SUCCESS);
    } catch (Exception e) {
      return new Status<>(table.getOriginatingElement(), e.getMessage(), Status.Result.FAILURE);
    }
  }

  public static class Status<R> {
    public enum Result {
      SUCCESS, FAILURE
    }

    public final R originatingElement;
    public final String errorMessage;
    public final Result result;

    public Status(R originatingElement, String errorMessage, Result result) {
      this.originatingElement = originatingElement;
      this.errorMessage = errorMessage;
      this.result = result;
    }
  }
}
