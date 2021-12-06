package app.cash.sqldelight.intellij

import app.cash.sqldelight.core.lang.psi.StmtIdentifierMixin
import com.alecstrong.sql.psi.core.psi.SqlColumnAlias
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlCteTableName
import com.alecstrong.sql.psi.core.psi.SqlTableAlias
import com.alecstrong.sql.psi.core.psi.SqlTableName
import com.alecstrong.sql.psi.core.psi.SqlViewName
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement

class SqlDelightFindUsagesProvider : FindUsagesProvider {
  override fun getWordsScanner() = null
  override fun getNodeText(element: PsiElement, useFullName: Boolean) = element.text
  override fun getDescriptiveName(element: PsiElement) = element.text
  override fun getHelpId(psiElement: PsiElement) = null

  override fun getType(element: PsiElement): String {
    return when (element) {
      is StmtIdentifierMixin -> "query"
      is SqlTableName -> "table"
      is SqlColumnName -> "column"
      is SqlTableAlias -> "table alias"
      is SqlColumnAlias -> "column alias"
      is SqlCteTableName -> "common table"
      is SqlViewName -> "view"
      else -> throw IllegalArgumentException("Unexpected type $element")
    }
  }

  override fun canFindUsagesFor(element: PsiElement): Boolean {
    return when (element) {
      is StmtIdentifierMixin, is SqlTableName, is SqlColumnName, is SqlTableAlias,
      is SqlColumnAlias, is SqlViewName, is SqlCteTableName -> true
      else -> false
    }
  }
}
