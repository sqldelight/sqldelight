package com.alecstrong.sqlite.android;

import com.alecstrong.sqlite.android.model.Column;
import com.alecstrong.sqlite.android.model.SqlStmt;
import com.alecstrong.sqlite.android.model.Table;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import javax.lang.model.element.Modifier;

public class SqliteCompiler<T> {
  public static String getOutputDirectory() {
    return Table.outputDirectory;
  }

  public static String getFileExtension() {
    return "sqlite2";
  }

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

      typeSpec.addType(MapperSpec.builder(table).build())
          .addType(MarshalSpec.builder(table).build());

      JavaFile javaFile = JavaFile.builder(table.getPackageName(), typeSpec.build()).build();
      File outputDirectory = table.getFileDirectory();
      outputDirectory.mkdirs();
      File outputFile = new File(outputDirectory, table.fileName());
      outputFile.createNewFile();
      javaFile.writeTo(new PrintStream(new FileOutputStream(outputFile)));

      return new Status<T>(table.getOriginatingElement(), "", Status.Result.SUCCESS);
    } catch (SqlitePluginException e) {
      return new Status<T>((T) e.originatingElement, e.getMessage(), Status.Result.FAILURE);
    } catch (IOException e) {
      return new Status<T>(table.getOriginatingElement(), e.getMessage(), Status.Result.FAILURE);
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
