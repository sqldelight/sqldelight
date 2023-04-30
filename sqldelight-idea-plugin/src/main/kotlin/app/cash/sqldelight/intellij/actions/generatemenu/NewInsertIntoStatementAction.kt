package app.cash.sqldelight.intellij.actions.generatemenu

import app.cash.sqldelight.intellij.actions.generatemenu.ui.InsertIntoStatementOptionsDialog
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper

internal class NewInsertIntoStatementAction : BaseSqlDelightGenerateAction(InsertIntoHandler()) {

  class InsertIntoHandler : BaseStatementGenerationHandler<InsertStatementOptions>() {

    override fun displayOptions(project: Project, context: StatementGenerationContext): InsertStatementOptions? {
      val optionsDialog = InsertIntoStatementOptionsDialog(project, context)

      optionsDialog.show()

      return if (optionsDialog.exitCode == DialogWrapper.OK_EXIT_CODE) {
        optionsDialog.getInsertStatementOptions()
      } else {
        null
      }
    }

    override fun generateStatement(
      sqlBuilder: SQLStatementBuilder,
      result: InsertStatementOptions
    ): String {
      return sqlBuilder.buildInsert(result)
    }

  }

}
