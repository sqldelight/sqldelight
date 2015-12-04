package com.alecstrong.sqlite.android;

import com.alecstrong.sqlite.android.model.Column;
import com.alecstrong.sqlite.android.model.SqlStmt;
import com.alecstrong.sqlite.android.model.Table;
import com.google.common.base.Joiner;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;

public final class SqliteCompiler<T> {
  public static final String TABLE_NAME = "TABLE_NAME";
  public static final String KEY_VALUE_KEY_COLUMN = "key";
  public static final String KEY_VALUE_VALUE_COLUMN = "value";

  public static String getOutputDirectory() {
    return TableGenerator.outputDirectory;
  }

  public static String getFileExtension(){
    return "sq";
  }

  public static String interfaceName(String sqliteFileName) {
    return sqliteFileName + "Model";
  }

  @SuppressWarnings("unchecked") // originating elements on exceptions originate from tables.
  public Status<T> write(TableGenerator<T, ?, ?, ?, ?> tableGenerator) {
    // TODO: Get rid of this in favour of proper antlr error handling. This is already done on the gradle side.
    if (tableGenerator == null) {
      return new Status<T>(null, "Expected but did not find package statement",
          Status.Result.FAILURE);
    }
    Table<T> table = tableGenerator.table();
    try {
      TypeSpec.Builder typeSpec = TypeSpec.interfaceBuilder(tableGenerator.interfaceName())
          .addModifiers(Modifier.PUBLIC);
      if (table != null) {
        typeSpec.addField(FieldSpec.builder(ClassName.get(String.class), TABLE_NAME)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .initializer("$S", table.sqlTableName())
            .build());

        for (Column<T> column : table.getColumns()) {
          typeSpec.addField(FieldSpec.builder(ClassName.get(String.class), column.fieldName())
              .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
              .initializer("$S", column.columnName())
              .build());

          MethodSpec.Builder methodSpec = MethodSpec.methodBuilder(column.methodName())
              .returns(column.getJavaType())
              .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
          if (column.isNullable()) {
            methodSpec.addAnnotation(ClassName.get("android.support.annotation", "Nullable"));
          }
          typeSpec.addMethod(methodSpec.build());
        }

        if (table.isKeyValue()) {
          typeSpec.addField(keyValueQuery(table));
        }

      typeSpec.addType(MapperSpec.builder(table).build())
          .addType(MarshalSpec.builder(table).build());

      }

      for (SqlStmt<T> sqlStmt : tableGenerator.sqliteStatements()) {
        typeSpec.addField(
            FieldSpec.builder(ClassName.get(String.class), sqlStmt.getIdentifier())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$S", sqlStmt.stmt)
                .build());
      }

      JavaFile javaFile = JavaFile.builder(tableGenerator.packageName(), typeSpec.build()).build();
      File outputDirectory = tableGenerator.getFileDirectory();
      outputDirectory.mkdirs();
      File outputFile = new File(outputDirectory, tableGenerator.fileName());
      outputFile.createNewFile();
      javaFile.writeTo(new PrintStream(new FileOutputStream(outputFile)));

      return new Status<T>(tableGenerator.getOriginatingElement(), "", Status.Result.SUCCESS);
    } catch (SqlitePluginException e) {
      return new Status<T>((T) e.originatingElement, e.getMessage(), Status.Result.FAILURE);
    } catch (IOException e) {
      return new Status<T>(tableGenerator.getOriginatingElement(), e.getMessage(),
          Status.Result.FAILURE);
    }
  }

  FieldSpec keyValueQuery(Table<T> table) {
    List<String> keys = new ArrayList<String>();
    for (Column<T> column : table.getColumns()) {
      keys.add("'" + column.columnName() + "'");
    }
    return FieldSpec.builder(ClassName.get(String.class), "QUERY", //
        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .initializer("\"SELECT * FROM " + table.sqlTableName() + " WHERE key IN ($L)\"",
            Joiner.on(',').join(keys))
        .build();
  }

  public static final class Status<R> {
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
