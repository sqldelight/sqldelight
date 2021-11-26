package app.cash.sqldelight.intellij.run

import app.cash.sqldelight.core.psi.SqlDelightStmtClojureStmtList
import app.cash.sqldelight.intellij.util.dialectPreset
import com.alecstrong.sql.psi.core.DialectPreset
import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.alecstrong.sql.psi.core.psi.SqlStmtList
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil

internal class SqlDelightRunAnnotator : Annotator {

  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (!element.isValidParent || element !is SqlStmt) {
      return
    }

    if (PsiTreeUtil.hasErrorElements(element)) {
      return
    }
    val project = element.project
    val connectionOptions = ConnectionOptions(project)
    val annotator = when (project.dialectPreset) {
      DialectPreset.SQLITE_3_18,
      DialectPreset.SQLITE_3_24,
      DialectPreset.SQLITE_3_25 -> RunSqliteAnnotator(holder, connectionOptions)
      else -> PsiElementVisitor.EMPTY_VISITOR
    }
    element.accept(annotator)
  }

  private val PsiElement.isValidParent: Boolean
    get() = parent is SqlStmtList || parent is SqlDelightStmtClojureStmtList
}
