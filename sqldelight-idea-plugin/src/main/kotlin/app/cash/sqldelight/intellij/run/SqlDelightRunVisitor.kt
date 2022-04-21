package app.cash.sqldelight.intellij.run

import app.cash.sqldelight.core.SqlDelightProjectService
import app.cash.sqldelight.dialect.api.ConnectionManager
import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.alecstrong.sql.psi.core.psi.SqlVisitor
import com.intellij.icons.AllIcons
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.markup.GutterIconRenderer
import javax.swing.Icon

internal class SqlDelightRunVisitor(
  private val holder: AnnotationHolder,
  private val connectionOptions: ConnectionOptions,
) : SqlVisitor() {

  override fun visitStmt(o: SqlStmt) {
    val connectionManager = SqlDelightProjectService.getInstance(o.project).dialect.connectionManager ?: return
    if (connectionOptions.selectedOption.isEmpty()) return

    holder.newAnnotation(HighlightSeverity.INFORMATION, "")
      .gutterIconRenderer(RunSqliteStatementGutterIconRenderer(o, connectionManager))
      .create()
  }

  private data class RunSqliteStatementGutterIconRenderer(
    private val stmt: SqlStmt,
    private val connectionManager: ConnectionManager
  ) : GutterIconRenderer() {
    override fun isNavigateAction() = true
    override fun getIcon(): Icon = AllIcons.RunConfigurations.TestState.Run
    override fun getTooltipText(): String = "Run statement"
    override fun getClickAction() = RunSqlAction(stmt, connectionManager)
  }
}
