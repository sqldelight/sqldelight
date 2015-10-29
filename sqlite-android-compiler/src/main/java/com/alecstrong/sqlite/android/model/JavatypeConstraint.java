package com.alecstrong.sqlite.android.model;

import com.alecstrong.sqlite.android.SqlitePluginException;
import com.alecstrong.sqlite.android.util.JavaParserUtil;
import com.google.common.base.Joiner;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JavatypeConstraint<T> extends ColumnConstraint<T> {
  private static final Map<String, TypeName> recognizedTypes = new LinkedHashMap<>();

  static {
    recognizedTypes.put("int", TypeName.INT);
    recognizedTypes.put("Integer", TypeName.INT.box());
    recognizedTypes.put("long", TypeName.LONG);
    recognizedTypes.put("Long", TypeName.LONG.box());
    recognizedTypes.put("short", TypeName.SHORT);
    recognizedTypes.put("Short", TypeName.SHORT.box());
    recognizedTypes.put("double", TypeName.DOUBLE);
    recognizedTypes.put("Double", TypeName.DOUBLE.box());
    recognizedTypes.put("float", TypeName.FLOAT);
    recognizedTypes.put("Float", TypeName.FLOAT.box());
    recognizedTypes.put("boolean", TypeName.BOOLEAN);
    recognizedTypes.put("Boolean", TypeName.BOOLEAN.box());
    recognizedTypes.put("String", ClassName.bestGuess("java.lang.String"));
    recognizedTypes.put("byte[]", ArrayTypeName.of(TypeName.BYTE));
  }

  private final String javatype;

  boolean isEnum;

  public JavatypeConstraint(String javatype, T originatingElement) {
    super(originatingElement);
    // Trim surrounding quotes if there are any.
    this.javatype = javatype.startsWith("\'") && javatype.endsWith("\'")
        ? javatype.substring(1, javatype.length() - 1)
        : javatype;
  }

  public void checkIsEnum(String projectPath) {
    if (javatype.contains("[") || javatype.contains("<")) {
      // If its an array or template it's not an enum.
      isEnum = false;
    } else {
      List<String> path = new ArrayList<>();
      List<String> classType = new ArrayList<>();
      for (String name : javatype.split("\\.")) {
        if (name.charAt(0) >= 'A' && name.charAt(0) <= 'Z') {
          // Class type.
          classType.add(name);
        } else {
          // File prefix.
          path.add(name);
        }
      }
      isEnum = JavaParserUtil.hasEnum(projectPath + "src/main/java/"
            + Joiner.on('/').join(path) + '/' + classType.get(0) + ".java", classType);
    }
  }

  public TypeName getJavatype() {
    try {
      TypeName known = recognizedTypes.get(javatype);
      return known == null ? ClassName.bestGuess(javatype) : known;
    } catch (Exception e) {
      throw new SqlitePluginException(getOriginatingElement(), "Unknown type " + javatype);
    }
  }

  public boolean isHandledType() {
    return recognizedTypes.containsKey(javatype) || isEnum;
  }
}
