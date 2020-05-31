package com.squareup.sqldelight.core.lang.psi

import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.SqlParser
import com.alecstrong.sql.psi.core.SqlParserDefinition
import com.alecstrong.sql.psi.core.psi.SqlAnnotatedElement
import com.alecstrong.sql.psi.core.psi.SqlIdentifier
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.lang.PsiBuilderFactory
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.GeneratedMarkerVisitor
import com.intellij.psi.impl.source.tree.TreeElement
import com.squareup.sqldelight.core.lang.SqlDelightQueriesFile
import com.squareup.sqldelight.core.psi.SqlDelightStmtIdentifier

abstract class StmtIdentifierMixin(
  node: ASTNode
) : ASTWrapperPsiElement(node),
    SqlDelightStmtIdentifier,
    SqlAnnotatedElement {
  override fun getName() = identifier()?.text

  override fun setName(name: String): PsiElement {
    val parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(language) as SqlParserDefinition
    var builder = PsiBuilderFactory.getInstance().createBuilder(
        project, node, parserDefinition.createLexer(project), language, name
    )
    builder = GeneratedParserUtilBase.adapt_builder_(
        SqlTypes.IDENTIFIER, builder, SqlParser(), SqlParser.EXTENDS_SETS_)
    GeneratedParserUtilBase.ErrorState.get(builder).currentFrame = GeneratedParserUtilBase.Frame()

    SqlParser.identifier_real(builder, 0)
    val element = builder.treeBuilt
    (element as TreeElement).acceptTree(GeneratedMarkerVisitor())
    node.replaceChild(identifier()!!.node, element)
    return this
  }

  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    if (name != null && (containingFile as SqlDelightQueriesFile).sqliteStatements()
        .filterNot { it.identifier == this }
        .any { it.identifier.name == name }) {
      annotationHolder.createErrorAnnotation(this, "Duplicate SQL identifier")
    }
  }

  override fun identifier() = children.filterIsInstance<SqlIdentifier>().singleOrNull()
}
