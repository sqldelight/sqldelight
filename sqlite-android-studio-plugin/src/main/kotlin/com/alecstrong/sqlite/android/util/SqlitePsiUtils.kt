package com.alecstrong.sqlite.android.util

import com.alecstrong.sqlite.android.SQLiteParser
import com.alecstrong.sqlite.android.lang.SqliteLanguage
import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.tree.IElementType
import org.antlr.intellij.adaptor.lexer.ElementTypeFactory

object SqlitePsiUtils {
  fun createLeafFromText(project: Project, context: PsiElement?, text: String, type: IElementType) =
      (PsiFileFactory.getInstance(project) as PsiFileFactoryImpl)
          .createElementFromText(text, SqliteLanguage.INSTANCE, type, context)!!.getDeepestFirst()
}

private val RULES = SQLiteParser.ruleNames.asList()
private val TOKENS = SQLiteParser.tokenNames.asList()

internal fun ASTNode.childrenWithRules(vararg rules: Int) = getChildren(
    ElementTypeFactory.createRuleSet(SqliteLanguage.INSTANCE, RULES, *rules))

internal fun ASTNode.childrenWithTokens(vararg tokens: Int) = getChildren(
    ElementTypeFactory.createTokenSet(SqliteLanguage.INSTANCE, TOKENS, *tokens))