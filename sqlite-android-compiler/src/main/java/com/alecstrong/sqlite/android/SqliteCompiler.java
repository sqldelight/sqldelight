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
  @SuppressWarnings("unchecked") // originating elements on exceptions originate from tables.
  public Status<T> write(Table<T> table) {
    try {
      TypeSpec.Builder typeSpec = TypeSpec.interfaceBuilder(table.interfaceName())
          .addModifiers(Modifier.PUBLIC);

      for (Column<T> column : table.getColumns()) {
        typeSpec.addField(FieldSpec.builder(ClassName.get(String.class), column.fieldName())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .initializer("$S", column.columnName())
            .build());

        typeSpec.addMethod(MethodSpec.methodBuilder(column.methodName())
            .returns(column.getJavaType())
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .build());
      }

      for (SqlStmt<T> sqlStmt : table.getSqlStmts()) {
        typeSpec.addField(
            FieldSpec.builder(ClassName.get(String.class), sqlStmt.getIdentifier())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$S", sqlStmt.stmt)
                .build());
      }

      JavaFile javaFile = JavaFile.builder(table.getPackageName(), typeSpec.build()).build();
      javaFile.writeTo(table.getOutputDirectory());

      return new Status<>(table.getOriginatingElement(), "", Status.Result.SUCCESS);
    } catch (SqlitePluginException e) {
      return new Status<>((T) e.originatingElement, e.getMessage(), Status.Result.FAILURE);
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
