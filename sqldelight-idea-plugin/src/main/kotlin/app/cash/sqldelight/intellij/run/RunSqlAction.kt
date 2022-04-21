package app.cash.sqldelight.intellij.run

import app.cash.sqldelight.core.compiler.model.BindableQuery
import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.core.lang.SqlDelightFileType
import app.cash.sqldelight.core.lang.psi.StmtIdentifier
import app.cash.sqldelight.core.lang.util.findChildOfType
import app.cash.sqldelight.core.lang.util.range
import app.cash.sqldelight.core.lang.util.rawSqlText
import app.cash.sqldelight.dialect.api.ConnectionManager
import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespace

@VisibleForTesting
internal class RunSqlAction(
  private val stmt: SqlStmt,
  private val connectionManager: ConnectionManager,
  private val project: Project = stmt.project,
  private val executor: SqlDelightStatementExecutor = SqlDelightStatementExecutor(
    project,
    connectionManager,
  ),
  private val dialogFactory: ArgumentsInputDialog.Factory = ArgumentsInputDialogFactoryImpl()
) : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val sql = stmt.rawSqlText().trim().replace("\\s+".toRegex(), " ")

    val parameters = findParameters(stmt)
    val sqlStmt = if (parameters.isEmpty()) {
      sql
    } else {
      val dialog = dialogFactory.create(project, parameters)
      val ok = dialog.showAndGet()
      if (!ok) return

      bindParameters(sql, dialog.result) ?: return
    }
    val identifier = stmt.getPrevSiblingIgnoringWhitespace() as? StmtIdentifier
    executor.execute(sqlStmt, identifier)
  }

  private fun findParameters(
    sqlStmt: SqlStmt
  ): List<SqlParameter> {
    val bindableQuery = object : BindableQuery(null, sqlStmt) {
      override val id = 0
    }
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
      p.range to p.value
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
