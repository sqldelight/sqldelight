package com.alecstrong.sqlite.android.lang

import com.alecstrong.sqlite.android.SQLiteLexer
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.tree.IFileElementType
import org.antlr.intellij.adaptor.lexer.ElementTypeFactory
import org.antlr.intellij.adaptor.lexer.SimpleAntlrAdapter

class SqliteParserDefinition : ParserDefinition {
  private val file = IFileElementType(SqliteLanguage.INSTANCE)

  override fun createLexer(project: Project) =
      SimpleAntlrAdapter(SqliteLanguage.INSTANCE, SQLiteLexer(null))

  override fun createParser(project: Project) = SqliteParser()

  override fun getWhitespaceTokens() = ElementTypeFactory.createTokenSet(SqliteLanguage.INSTANCE,
      listOf(*SQLiteLexer.tokenNames), SQLiteLexer.SPACES)

  override fun getCommentTokens() = ElementTypeFactory.createTokenSet(SqliteLanguage.INSTANCE,
      listOf(*SQLiteLexer.tokenNames), SQLiteLexer.SINGLE_LINE_COMMENT,
      SQLiteLexer.MULTILINE_COMMENT)

  override fun getStringLiteralElements() = ElementTypeFactory.createTokenSet(
      SqliteLanguage.INSTANCE, listOf(*SQLiteLexer.tokenNames), SQLiteLexer.SINGLE_LINE_COMMENT,
      SQLiteLexer.STRING_LITERAL)

  override fun getFileNodeType() = file

  override fun createFile(viewProvider: FileViewProvider) = SqliteFile(viewProvider)

  override fun createElement(node: ASTNode) = node.asPSINode()

  override fun spaceExistanceTypeBetweenTokens(left: ASTNode, right: ASTNode) =
      ParserDefinition.SpaceRequirements.MAY
}
