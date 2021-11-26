package app.cash.sqldelight.intellij.run

import app.cash.sqldelight.core.compiler.model.NamedMutator
import app.cash.sqldelight.core.compiler.model.NamedQuery
import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.core.lang.SqlDelightFileType
import app.cash.sqldelight.core.lang.psi.StmtIdentifierMixin
import app.cash.sqldelight.core.lang.util.findChildOfType
import app.cash.sqldelight.core.lang.util.range
import app.cash.sqldelight.core.lang.util.rawSqlText
import app.cash.sqldelight.core.psi.SqlDelightStmtClojure
import app.cash.sqldelight.core.psi.SqlDelightStmtClojureStmtList
import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.alecstrong.sql.psi.core.psi.SqlStmtList
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.VisibleForTesting

@VisibleForTesting
internal class RunSqlAction(
  private val stmt: SqlStmt,
  private val project: Project = stmt.project,
  private val executor: SqlDelightStatementExecutor = SqlDelightStatementExecutor.getInstance(
    project
  ),
  private val dialogFactory: ArgumentsInputDialog.Factory = ArgumentsInputDialogFactoryImpl()
) : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val sql = stmt.rawSqlText().trim().replace("\\s+".toRegex(), " ")

    val identifier = stmt.identifier
    val parameters = identifier?.let { findParameters(stmt, it) } ?: emptyList()
    val sqlStmt = if (parameters.isEmpty()) {
      sql
    } else {
      val dialog = dialogFactory.create(project, parameters)
      val ok = dialog.showAndGet()
      if (!ok) return

      bindParameters(sql, dialog.result) ?: return
    }
    executor.execute(sqlStmt)
  }

  private val SqlStmt.identifier: StmtIdentifierMixin?
    get() {
      return when (parent) {
        is SqlStmtList -> prevVisibleSibling() as? StmtIdentifierMixin
        is SqlDelightStmtClojureStmtList -> {
          val stmtClojure = PsiTreeUtil.getParentOfType(this, SqlDelightStmtClojure::class.java)
          stmtClojure?.stmtIdentifierClojure as? StmtIdentifierMixin
        }
        else -> null
      }
    }

  private fun PsiElement.prevVisibleSibling(): PsiElement? {
    return generateSequence(prevSibling) { it.prevSibling }
      .firstOrNull { it !is PsiWhiteSpace && it !is PsiComment }
  }

  private fun findParameters(
    sqlStmt: SqlStmt,
    identifier: StmtIdentifierMixin
  ): List<SqlParameter> {
    val bindableQuery = when {
      sqlStmt.compoundSelectStmt != null -> NamedQuery(
        identifier.name!!, sqlStmt.compoundSelectStmt!!, identifier
      )
      sqlStmt.deleteStmtLimited != null -> NamedMutator.Delete(
        sqlStmt.deleteStmtLimited!!, identifier
      )
      sqlStmt.insertStmt != null -> NamedMutator.Insert(
        sqlStmt.insertStmt!!, identifier
      )
      sqlStmt.updateStmtLimited != null -> NamedMutator.Update(
        sqlStmt.updateStmtLimited!!, identifier
      )
      else -> null
    } ?: return emptyList()
    val offset = sqlStmt.textOffset
    val argumentList: List<IntRange> = bindableQuery.arguments
      .flatMap { it.bindArgs }
      .map {
        val textRange = it.range
        IntRange(textRange.first - offset, textRange.last - offset)
      }
    val parameters: List<String> = bindableQuery.parameters
      .map { it.name }
    return argumentList.zip(parameters) { range, name ->
      SqlParameter(
        name = name,
        range = range
      )
    }
  }

  private fun bindParameters(
    sql: String,
    parameters: List<SqlParameter>
  ): String? {
    val replacements = parameters.mapNotNull { p ->
      if (p.value.isEmpty()) {
        return@mapNotNull null
      }
      p.range to "'${p.value}'"
    }
    if (replacements.isEmpty()) {
      return null
    }

    val factory = PsiFileFactory.getInstance(project)
    return runReadAction {
      val dummyFile = factory.createFileFromText(
        "_Dummy_.${SqlDelightFileType.EXTENSION}",
        SqlDelightFileType,
        sql
      ) as SqlDelightFile
      val stmt = dummyFile.findChildOfType<SqlStmt>()
      stmt?.rawSqlText(replacements)
    }
  }
}