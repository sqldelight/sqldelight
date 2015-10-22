package com.alecstrong.sqlite.android;

import com.alecstrong.sqlite.android.lang.SqliteLanguage;
import com.alecstrong.sqlite.android.model.Table;
import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.psi.PsiFile;
import java.util.Arrays;
import java.util.List;
import org.antlr.intellij.adaptor.lexer.ElementTypeFactory;
import org.antlr.intellij.adaptor.lexer.RuleElementType;
import org.jetbrains.annotations.Nullable;

public class SqlDocumentAnnotator extends ExternalAnnotator<PsiFile, PsiFile> {
  @Nullable @Override public PsiFile collectInformation(PsiFile file) {
    return file;
  }

  @Nullable @Override public PsiFile doAnnotate(PsiFile collectedInfo) {
    SqliteCompiler.write(forFile(collectedInfo));
    return collectedInfo;
  }

  @Override public void apply(PsiFile file, PsiFile annotationResult, AnnotationHolder holder) {
    super.apply(file, annotationResult, holder);
  }

  private static Table forFile(PsiFile file) {
    List<RuleElementType> elementTypes = ElementTypeFactory.getRuleElementTypes(SqliteLanguage.INSTANCE, Arrays
        .asList(SQLiteParser.ruleNames));
    ASTNode parse = childrenForRules(file.getNode(), SQLiteParser.RULE_parse)[0];
    ASTNode sqlStatementList = childrenForRules(parse, SQLiteParser.RULE_sql_stmt_list)[0];
    ASTNode sqlStatement = childrenForRules(sqlStatementList, SQLiteParser.RULE_sql_stmt)[0];
    ASTNode createStatement = childrenForRules(sqlStatement, SQLiteParser.RULE_create_table_stmt)[0];

    return new Table("com.derp.derp", "Derp");
  }

  private static final List<String> RULES = Arrays.asList(SQLiteParser.ruleNames);
  private static ASTNode[] childrenForRules(ASTNode node, int... rules) {
    return node.getChildren(ElementTypeFactory.createRuleSet(SqliteLanguage.INSTANCE, RULES, rules));
  }
}
