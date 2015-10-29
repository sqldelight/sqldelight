package com.alecstrong.sqlite.android.model;

import com.alecstrong.sqlite.android.JavaLexer;
import com.alecstrong.sqlite.android.JavaParser;
import com.alecstrong.sqlite.android.JavaParser.ClassDeclarationContext;
import com.alecstrong.sqlite.android.JavaParser.EnumDeclarationContext;
import com.alecstrong.sqlite.android.JavaParser.InterfaceDeclarationContext;
import com.alecstrong.sqlite.android.SqlitePluginException;
import com.google.common.base.Joiner;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;

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

  private boolean isEnum;

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
      try {
        JavaLexer javaLexer = new JavaLexer(new ANTLRFileStream(projectPath + "src/main/java/"
            + Joiner.on('/').join(path) + '/' + classType.get(0) + ".java"));
        JavaParser javaParser = new JavaParser(new CommonTokenStream(javaLexer));

        isEnum = hasEnum(classType, 0, javaParser.compilationUnit().typeDeclaration(0));
      } catch (IOException e) {
        isEnum = false;
      }
    }
  }

  private boolean hasEnum(List<String> enumPath, int offset,
      JavaParser.TypeDeclarationContext node) {
    if (hasEnum(enumPath, offset, node.interfaceDeclaration())) return true;
    if (hasEnum(enumPath, offset, node.enumDeclaration())) return true;
    if (hasEnum(enumPath, offset, node.classDeclaration())) return true;
    return false;
  }

  private boolean hasEnum(List<String> enumPath, int offset, ParserRuleContext node) {
    List<JavaParser.ClassBodyDeclarationContext> classDeclarations = Collections.emptyList();
    if (node instanceof InterfaceDeclarationContext) {
      if (!((InterfaceDeclarationContext) node).Identifier().getText().equals(enumPath.get(offset))) {
        return false;
      }
      if (offset == enumPath.size() - 1) return false;
      for (JavaParser.InterfaceBodyDeclarationContext interfaceBody :
          ((InterfaceDeclarationContext) node).interfaceBody().interfaceBodyDeclaration()) {
        if (hasEnum(enumPath, offset + 1,
            interfaceBody.interfaceMemberDeclaration().classDeclaration())) {
          return true;
        }
        if (hasEnum(enumPath, offset + 1,
            interfaceBody.interfaceMemberDeclaration().interfaceDeclaration())) {
          return true;
        }
        if (hasEnum(enumPath, offset + 1,
            interfaceBody.interfaceMemberDeclaration().enumDeclaration())) {
          return true;
        }
      }
      return false;
    } else if (node instanceof ClassDeclarationContext) {
      if (!((ClassDeclarationContext) node).Identifier().getText().equals(enumPath.get(offset))) {
        return false;
      }
      if (offset == enumPath.size() - 1) return false;
      classDeclarations = ((ClassDeclarationContext) node).classBody().classBodyDeclaration();
    } else if (node instanceof EnumDeclarationContext) {
      if (!((EnumDeclarationContext) node).Identifier().getText().equals(enumPath.get(offset))) {
        return false;
      }
      if (offset == enumPath.size() - 1) return true;
      classDeclarations =
          ((EnumDeclarationContext) node).enumBodyDeclarations().classBodyDeclaration();
    }
    for (JavaParser.ClassBodyDeclarationContext classDeclaration : classDeclarations) {
      for (JavaParser.BlockStatementContext block : classDeclaration.block().blockStatement()) {
        if (hasEnum(enumPath, offset + 1, block.typeDeclaration())) return true;
      }
    }
    return false;
  }

  public TypeName getJavatype() {
    try {
      TypeName known = recognizedTypes.get(javatype);
      return known == null ? ClassName.bestGuess(javatype) : known;
    } catch (Exception e) {
      throw new SqlitePluginException(getOriginatingElement(), "Unknown type " + javatype);
    }
  }
}
