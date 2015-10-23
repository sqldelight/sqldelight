package com.alecstrong.sqlite.android;

import com.alecstrong.sqlite.android.model.Column;
import com.alecstrong.sqlite.android.model.Table;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import java.io.File;
import javax.lang.model.element.Modifier;

public class SqliteCompiler {
  public static void write(Table table, File directory) {
    TypeSpec.Builder typeSpec = TypeSpec.interfaceBuilder(table.interfaceName())
        .addModifiers(Modifier.PUBLIC);

    for (Column column : table.columns) {
      typeSpec.addField(FieldSpec.builder(ClassName.get(String.class), column.name.toUpperCase())
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
          .initializer("$S", column.name)
          .build());
    }

    JavaFile javaFile = JavaFile.builder(table.packageName, typeSpec.build()).build();

    try {
      javaFile.writeTo(directory);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
