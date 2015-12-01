package com.alecstrong.sqlite.android.generating;

import com.alecstrong.sqlite.android.SQLiteParser;
import com.alecstrong.sqlite.android.lang.SqliteLanguage;
import com.alecstrong.sqlite.android.lang.SqliteTokenTypes;
import com.alecstrong.sqlite.android.model.Column;
import com.alecstrong.sqlite.android.model.ColumnConstraint;
import com.alecstrong.sqlite.android.model.NotNullConstraint;
import com.alecstrong.sqlite.android.model.SqlStmt.Replacement;
import com.google.common.base.Joiner;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.antlr.intellij.adaptor.lexer.ElementTypeFactory;
import org.antlr.intellij.adaptor.lexer.TokenElementType;

public class TableGenerator extends
    com.alecstrong.sqlite.android.TableGenerator<ASTNode, ASTNode, ASTNode, ASTNode, ASTNode> {
  private static final List<String> RULES = Arrays.asList(SQLiteParser.ruleNames);
  private static final List<String> TOKENS = Arrays.asList(SQLiteParser.tokenNames);

  static TableGenerator create(PsiFile file) {
    ASTNode parse = childrenForRules(file.getNode(), SQLiteParser.RULE_parse)[0];
    if (parse == null
        || parse.getFirstChildNode() == null
        || parse.getFirstChildNode().getElementType() != SqliteTokenTypes.RULE_ELEMENT_TYPES.get(
        SQLiteParser.RULE_package_stmt)) {
      return null;
    }
    String packageName =
        getPackageName(childrenForRules(parse, SQLiteParser.RULE_package_stmt)[0]);
    return new TableGenerator(parse, packageName, file.getName(),
        ModuleUtil.findModuleForPsiElement(file).getModuleFile().getParent().getPath() + "/");
  }

  private TableGenerator(ASTNode rootElement, String packageName, String fileName,
      String projectPath) {
    super(rootElement, packageName, fileName, projectPath);
  }

  @Override protected Iterable<ASTNode> sqlStatementElements(ASTNode originatingElement) {
    ASTNode sqlStatementList =
        childrenForRules(originatingElement, SQLiteParser.RULE_sql_stmt_list)[0];
    return Arrays.asList(childrenForRules(sqlStatementList, SQLiteParser.RULE_sql_stmt));
  }

  @Override protected ASTNode tableElement(ASTNode originatingElement) {
    ASTNode sqlStatementList =
        childrenForRules(originatingElement, SQLiteParser.RULE_sql_stmt_list)[0];
    ASTNode[] result = childrenForRules(sqlStatementList, SQLiteParser.RULE_create_table_stmt);
    return result.length == 1 ? result[0] : null;
  }

  @Override protected String identifier(ASTNode sqlStatementElement) {
    ASTNode[] result = childrenForTokens(sqlStatementElement, SQLiteParser.IDENTIFIER);
    return result.length == 1 ? result[0].getText() : null;
  }

  @Override protected Iterable<ASTNode> columnElements(ASTNode tableElement) {
    return Arrays.asList(childrenForRules(tableElement, SQLiteParser.RULE_column_def));
  }

  @Override protected String tableName(ASTNode tableElement) {
    return childrenForRules(tableElement, SQLiteParser.RULE_table_name)[0].getText();
  }

  @Override protected boolean isKeyValue(ASTNode tableElement) {
    return childrenForTokens(tableElement, SQLiteParser.K_KEY_VALUE).length == 1;
  }

  @Override protected String columnName(ASTNode columnElement) {
    return childrenForRules(columnElement, SQLiteParser.RULE_column_name)[0].getText();
  }

  @Override protected String classLiteral(ASTNode columnElement) {
    ASTNode typeNode = childrenForRules(columnElement, SQLiteParser.RULE_type_name)[0];
    ASTNode[] className = childrenForRules(typeNode, SQLiteParser.RULE_sqlite_class_name);
    return className.length == 1
        ? childrenForTokens(className[0], SQLiteParser.STRING_LITERAL)[0].getText()
        : null;
  }

  @Override protected String typeName(ASTNode columnElement) {
    ASTNode typeNode = childrenForRules(columnElement, SQLiteParser.RULE_type_name)[0];
    ASTNode[] className = childrenForRules(typeNode, SQLiteParser.RULE_sqlite_class_name);
    return className.length == 1
        ? className[0].getFirstChildNode().getText()
        : typeNode.getText();
  }

  @Override protected Replacement replacementFor(ASTNode columnElement, Column.Type type) {
    ASTNode typeNode = childrenForRules(columnElement, SQLiteParser.RULE_type_name)[0];
    return new Replacement(typeNode.getTextRange().getStartOffset(),
        typeNode.getTextRange().getEndOffset(), type.replacement);
  }

  @Override protected Iterable<ASTNode> constraintElements(ASTNode columnElement) {
    return Arrays.asList(childrenForRules(columnElement, SQLiteParser.RULE_column_constraint));
  }

  @Override protected ColumnConstraint<ASTNode> constraintFor(ASTNode constraintElement,
      List<Replacement> replacements) {
    for (ASTNode child : constraintElement.getChildren(null)) {
      IElementType elementType = child.getElementType();
      if (!(elementType instanceof TokenElementType)) continue;
      switch (((TokenElementType) elementType).getType()) {
        case SQLiteParser.K_NOT:
          return new NotNullConstraint<ASTNode>(constraintElement);
      }
    }
    return null;
  }

  @Override protected String text(ASTNode sqliteStatementElement) {
    if (sqliteStatementElement.getElementType() == SqliteTokenTypes.RULE_ELEMENT_TYPES.get(
        SQLiteParser.RULE_sql_stmt)) {
      return sqliteStatementElement.getLastChildNode().getText();
    } else {
      return sqliteStatementElement.getText();
    }
  }

  @Override protected int startOffset(ASTNode sqliteStatementElement) {
    if (sqliteStatementElement.getElementType() == SqliteTokenTypes.RULE_ELEMENT_TYPES.get(
        SQLiteParser.RULE_create_table_stmt)) {
      return sqliteStatementElement.getStartOffset();
    }
    return sqliteStatementElement.getLastChildNode().getStartOffset();
  }

  private static String getPackageName(ASTNode packageNode) {
    List<String> names = new ArrayList<String>();
    for (ASTNode name : childrenForRules(packageNode, SQLiteParser.RULE_name)) {
      names.add(name.getText());
    }
    return Joiner.on('.').join(names);
  }

  private static ASTNode[] childrenForRules(ASTNode node, int... rules) {
    return node.getChildren(
        ElementTypeFactory.createRuleSet(SqliteLanguage.INSTANCE, RULES, rules));
  }

  public static ASTNode[] childrenForTokens(ASTNode node, int... tokens) {
    return node.getChildren(
        ElementTypeFactory.createTokenSet(SqliteLanguage.INSTANCE, TOKENS, tokens));
  }
}
