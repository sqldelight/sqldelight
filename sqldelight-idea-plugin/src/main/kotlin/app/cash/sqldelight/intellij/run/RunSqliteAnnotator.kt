package app.cash.sqldelight.intellij.run

import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.alecstrong.sql.psi.core.psi.SqlVisitor
import com.intellij.icons.AllIcons
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.editor.markup.GutterIconRenderer
import javax.swing.Icon

internal class RunSqliteAnnotator(
  private val holder: AnnotationHolder,
  private val connectionOptions: ConnectionOptions,
) : SqlVisitor() {

  override fun visitStmt(o: SqlStmt) {
    if (connectionOptions.connectionType != ConnectionType.FILE) {
      return
    }

    val filePath = connectionOptions.filePath
    if (filePath.isEmpty()) {
      return
    }

    holder.newAnnotation(HighlightSeverity.INFORMATION, "")
      .gutterIconRenderer(RunSqliteStatementGutterIconRenderer(o))
      .create()
  }

  private data class RunSqliteStatementGutterIconRenderer(
    private val stmt: SqlStmt,
  ) : GutterIconRenderer() {
    override fun getIcon(): Icon = AllIcons.RunConfigurations.TestState.Run

    override fun getTooltipText(): String = "Run statement"

    override fun getClickAction(): AnAction = RunSqlAction(stmt)
  }
}
