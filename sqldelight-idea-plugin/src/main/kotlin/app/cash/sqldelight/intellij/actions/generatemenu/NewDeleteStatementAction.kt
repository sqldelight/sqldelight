package app.cash.sqldelight.intellij.actions.generatemenu

import app.cash.sqldelight.intellij.actions.generatemenu.ui.DeleteStatementOptionsDialog
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper

internal class NewDeleteStatementAction : BaseSqlDelightGenerateAction(DeleteFromHandler()) {

  class DeleteFromHandler : BaseStatementGenerationHandler<DeleteStatementOptions>() {

    override fun displayOptions(project: Project, context: StatementGenerationContext): DeleteStatementOptions? {
      val optionsDialog = DeleteStatementOptionsDialog(project, context)

      optionsDialog.show()

      return if (optionsDialog.exitCode == DialogWrapper.OK_EXIT_CODE) {
        optionsDialog.getDeleteStatementOptions()
      } else {
        null
      }
    }

    override fun generateStatement(
      sqlBuilder: SQLStatementBuilder,
      result: DeleteStatementOptions
    ): String {
      return sqlBuilder.buildDelete(result)
    }

  }

}
