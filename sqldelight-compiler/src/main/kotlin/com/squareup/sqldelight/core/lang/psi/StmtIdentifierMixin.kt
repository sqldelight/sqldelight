package com.squareup.sqldelight.core.lang.psi

import com.alecstrong.sqlite.psi.core.SqliteAnnotationHolder
import com.alecstrong.sqlite.psi.core.SqliteParserDefinition
import com.alecstrong.sqlite.psi.core.parser.SqliteParser
import com.alecstrong.sqlite.psi.core.psi.SqliteAnnotatedElement
import com.alecstrong.sqlite.psi.core.psi.SqliteIdentifier
import com.alecstrong.sqlite.psi.core.psi.SqliteTypes
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.lang.PsiBuilderFactory
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.GeneratedMarkerVisitor
import com.intellij.psi.impl.source.tree.TreeElement
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.psi.SqlDelightStmtIdentifier

abstract class StmtIdentifierMixin(
  node: ASTNode
) : ASTWrapperPsiElement(node),
    SqlDelightStmtIdentifier,
    SqliteAnnotatedElement {
  override fun getName() = identifier()?.text

  override fun setName(name: String): PsiElement {
    val parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(language) as SqliteParserDefinition
    var builder = PsiBuilderFactory.getInstance().createBuilder(
        project, node, parserDefinition.createLexer(project), language, name
    )
    builder = GeneratedParserUtilBase.adapt_builder_(
        SqliteTypes.IDENTIFIER, builder, SqliteParser(), SqliteParser.EXTENDS_SETS_)

    SqliteParser.identifier_real(builder, 0)
    val element = builder.treeBuilt
    (element as TreeElement).acceptTree(GeneratedMarkerVisitor())
    node.replaceChild(identifier()!!.node, element)
    return this
  }

  override fun annotate(annotationHolder: SqliteAnnotationHolder) {
    if (name != null && (containingFile as SqlDelightFile).sqliteStatements()
        .filterNot { it.identifier == this }
        .any { it.identifier.name == name }) {
      annotationHolder.createErrorAnnotation(this, "Duplicate SQL identifier")
    }
  }

  override fun identifier() = children.filterIsInstance<SqliteIdentifier>().singleOrNull()
}
