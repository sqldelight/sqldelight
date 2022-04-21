package app.cash.sqldelight.intellij.run

import app.cash.sqldelight.core.compiler.model.BindableQuery
import app.cash.sqldelight.core.lang.psi.StmtIdentifier
import app.cash.sqldelight.core.lang.util.range
import app.cash.sqldelight.core.lang.util.rawSqlText
import app.cash.sqldelight.dialect.api.ConnectionManager
import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
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
    val parameters = findParameters(stmt)
    val replacements = if (parameters.isEmpty()) {
      emptyList()
    } else {
      val dialog = dialogFactory.create(project, parameters)
      if (!dialog.showAndGet() || dialog.result.any { it.value.isEmpty() }) return
      dialog.result.map { p -> p.range to p.value }
    }
    val sqlStmt = stmt.rawSqlText(replacements).trim().replace("\\s+".toRegex(), " ")
    val identifier = stmt.getPrevSiblingIgnoringWhitespace() as? StmtIdentifier
    executor.execute(sqlStmt, identifier)
  }

  private fun findParameters(
    sqlStmt: SqlStmt
  ): List<SqlParameter> {
    val bindableQuery = object : BindableQuery(null, sqlStmt) {
      override val id = 0
    }
    val argumentList: List<IntRange> = bindableQuery.arguments
      .flatMap { it.bindArgs }
      .map { it.range }
    val parameters: List<String> = bindableQuery.parameters.map { it.name }
    return argumentList.zip(parameters) { range, name ->
      SqlParameter(
        name = name,
        range = range
      )
    }
  }
}
