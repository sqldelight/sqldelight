package app.cash.sqldelight.intellij.run

import app.cash.sqldelight.core.psi.SqlDelightStmtClojureStmtList
import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.alecstrong.sql.psi.core.psi.SqlStmtList
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
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
    element.accept(SqlDelightRunVisitor(holder, connectionOptions))
  }

  private val PsiElement.isValidParent: Boolean
    get() = parent is SqlStmtList || parent is SqlDelightStmtClojureStmtList
}
