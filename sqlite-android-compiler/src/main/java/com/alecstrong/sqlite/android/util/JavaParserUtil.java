package com.alecstrong.sqlite.android.util;

import com.alecstrong.sqlite.android.JavaLexer;
import com.alecstrong.sqlite.android.JavaParser;
import java.io.IOException;
import java.util.List;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;

public class JavaParserUtil {
  /**
   * @return true if the given file path has an enum found at enum path (filePath will look like
   * com/sample/File.java and enumPath will contain a series of strings which lead to the enum.)
   */
  public static boolean hasEnum(String filePath, List<String> enumPath) {
    try {
      JavaLexer javaLexer = new JavaLexer(new ANTLRFileStream(filePath));
      JavaParser javaParser = new JavaParser(new CommonTokenStream(javaLexer));

      return hasEnum(enumPath, 0, javaParser.compilationUnit().typeDeclaration(0));
    } catch (IOException e) {
      return false;
    }
  }

  private static boolean hasEnum(List<String> enumPath, int offset,
      JavaParser.TypeDeclarationContext node) {
    return node != null && (hasEnum(enumPath, offset, node.interfaceDeclaration()) || hasEnum(
        enumPath, offset, node.enumDeclaration()) || hasEnum(enumPath, offset,
        node.classDeclaration()));
  }

  private static boolean hasEnum(List<String> enumPath, int offset,
      JavaParser.MemberDeclarationContext node) {
    return node != null && (hasEnum(enumPath, offset, node.interfaceDeclaration()) || hasEnum(
        enumPath, offset, node.enumDeclaration()) || hasEnum(enumPath, offset,
        node.classDeclaration()));
  }

  private static boolean hasEnum(List<String> enumPath, int offset,
      JavaParser.InterfaceMemberDeclarationContext node) {
    return node != null && (hasEnum(enumPath, offset, node.interfaceDeclaration()) || hasEnum(
        enumPath, offset, node.enumDeclaration()) || hasEnum(enumPath, offset,
        node.classDeclaration()));
  }

  private static boolean hasEnum(List<String> enumPath, int offset,
      JavaParser.InterfaceDeclarationContext node) {
    if (node == null) return false;
    if (!node.Identifier().getText()
        .equals(enumPath.get(offset))) {
      return false;
    }
    if (offset == enumPath.size() - 1) return false;
    if (node.interfaceBody() == null) return false;
    for (JavaParser.InterfaceBodyDeclarationContext interfaceBody :
        node.interfaceBody().interfaceBodyDeclaration()) {
      if (hasEnum(enumPath, offset + 1, interfaceBody.interfaceMemberDeclaration())) return true;
    }
    return false;
  }

  private static boolean hasEnum(List<String> enumPath, int offset,
      JavaParser.ClassDeclarationContext node) {
    if (node == null) return false;
    if (!node.Identifier().getText().equals(enumPath.get(offset))) {
      return false;
    }
    if (offset == enumPath.size() - 1) return false;
    if (node.classBody() == null) return false;
    for (JavaParser.ClassBodyDeclarationContext classDec : node.classBody()
        .classBodyDeclaration()) {
      if (hasEnum(enumPath, offset + 1, classDec.memberDeclaration())) return true;
    }
    return false;
  }

  private static boolean hasEnum(List<String> enumPath, int offset,
      JavaParser.EnumDeclarationContext node) {
    if (node == null) return false;
    if (!node.Identifier().getText().equals(enumPath.get(offset))) {
      return false;
    }
    if (offset == enumPath.size() - 1) return true;
    if (node.enumBodyDeclarations() == null) return false;
    for (JavaParser.ClassBodyDeclarationContext classDec : node.enumBodyDeclarations()
        .classBodyDeclaration()) {
      if (hasEnum(enumPath, offset + 1, classDec.memberDeclaration())) return true;
    }
    return false;
  }
}
