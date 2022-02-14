package app.cash.sqldelight.intellij.lang

import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.core.psi.SqlDelightImportStmt
import app.cash.sqldelight.core.psi.SqlDelightStmtIdentifier
import com.alecstrong.sql.psi.core.psi.AliasElement
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlFunctionName
import com.alecstrong.sql.psi.core.psi.SqlIdentifier
import com.alecstrong.sql.psi.core.psi.SqlTableName
import com.alecstrong.sql.psi.core.psi.SqlTypeName
import com.alecstrong.sql.psi.core.psi.SqlVisitor
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.HighlightVisitor
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.forEachDescendantOfType

class SqlDelightHighlightVisitor : SqlVisitor(), HighlightVisitor {

  private var myHolder: HighlightInfoHolder? = null

  override fun suitableForFile(file: PsiFile): Boolean {
    return file is SqlDelightFile
  }

  override fun visit(element: PsiElement) {
    when (element) {
      is AliasElement -> visitAliasElement(element)
      is SqlColumnName -> visitColumnName(element)
      is SqlFunctionName -> visitFunctionName(element)
      is SqlIdentifier -> visitIdentifier(element)
      is SqlTableName -> visitTableName(element)
      is SqlTypeName -> visitTypeName(element)
      is SqlDelightImportStmt -> visitImportStmt(element)
    }
  }

  override fun analyze(
    file: PsiFile,
    updateWholeFile: Boolean,
    holder: HighlightInfoHolder,
    action: Runnable
  ): Boolean {
    myHolder = holder
    try {
      action.run()
    } finally {
      myHolder = null
    }
    return true
  }

  override fun clone(): HighlightVisitor {
    return SqlDelightHighlightVisitor()
  }

  override fun visitColumnName(o: SqlColumnName) {
    val info = HighlightInfo.newHighlightInfo(createSymbolTypeInfo(SQL_COLUMN_NAME))
      .range(o)
      .create()
    myHolder?.add(info)
  }

  override fun visitFunctionName(o: SqlFunctionName) {
    val info = HighlightInfo.newHighlightInfo(createSymbolTypeInfo(SQL_FUNCTION_NAME))
      .range(o)
      .create()
    myHolder?.add(info)
  }

  override fun visitIdentifier(o: SqlIdentifier) {
    if (o.parent is SqlDelightStmtIdentifier) {
      val info = HighlightInfo.newHighlightInfo(createSymbolTypeInfo(SQL_STMT_IDENTIFIER_NAME))
        .range(o)
        .create()
      myHolder?.add(info)
    }
  }

  override fun visitTypeName(o: SqlTypeName) {
    val info = HighlightInfo.newHighlightInfo(createSymbolTypeInfo(SQL_TYPE_NAME))
      .range(o)
      .create()
    myHolder?.add(info)
  }

  override fun visitTableName(o: SqlTableName) {
    val info = HighlightInfo.newHighlightInfo(createSymbolTypeInfo(SQL_TABLE_NAME))
      .range(o)
      .create()
    myHolder?.add(info)
  }

  override fun visitAliasElement(o: AliasElement) {
    val info = HighlightInfo.newHighlightInfo(createSymbolTypeInfo(SQL_ALIAS))
      .range(o)
      .create()
    myHolder?.add(info)
  }

  private fun visitImportStmt(o: SqlDelightImportStmt) {
    o.forEachDescendantOfType<LeafPsiElement> {
      if (it.textMatches("import")) {
        val info = HighlightInfo.newHighlightInfo(createSymbolTypeInfo(SQL_TYPE_NAME))
          .range(it.textRange)
          .create()
        myHolder?.add(info)
      }
    }
  }

  private fun createSymbolTypeInfo(attributesKey: TextAttributesKey): HighlightInfoType {
    return HighlightInfoType.HighlightInfoTypeImpl(
      HighlightInfoType.SYMBOL_TYPE_SEVERITY, attributesKey, false
    )
  }

  companion object {
    val SQL_ALIAS = TextAttributesKey.createTextAttributesKey(
      "SQL.ALIAS",
      DefaultLanguageHighlighterColors.LOCAL_VARIABLE
    )
    val SQL_COLUMN_NAME = TextAttributesKey.createTextAttributesKey(
      "SQL.COLUMN_NAME",
      DefaultLanguageHighlighterColors.INSTANCE_FIELD
    )
    val SQL_FUNCTION_NAME = TextAttributesKey.createTextAttributesKey(
      "SQL.FUNCTION_NAME",
      DefaultLanguageHighlighterColors.STATIC_METHOD
    )
    val SQL_STMT_IDENTIFIER_NAME = TextAttributesKey.createTextAttributesKey(
      "SQL.STMT_IDENTIFIER_NAME",
      DefaultLanguageHighlighterColors.INSTANCE_METHOD
    )
    val SQL_TABLE_NAME = TextAttributesKey.createTextAttributesKey(
      "SQL.TABLE_NAME",
      DefaultLanguageHighlighterColors.CLASS_NAME
    )
    val SQL_TYPE_NAME = TextAttributesKey.createTextAttributesKey(
      "SQL.TYPE_NAME",
      DefaultLanguageHighlighterColors.KEYWORD
    )
  }
}
