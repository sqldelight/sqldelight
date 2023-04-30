package app.cash.sqldelight.intellij.actions.generatemenu

import app.cash.sqldelight.intellij.actions.generatemenu.ui.UpdateStatementOptionsDialog
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper

internal class NewUpdateStatementAction : BaseSqlDelightGenerateAction(UpdateHandler()) {

  class UpdateHandler : BaseStatementGenerationHandler<UpdateStatementOptions>() {

    override fun displayOptions(project: Project, context: StatementGenerationContext): UpdateStatementOptions? {
      val optionsDialog = UpdateStatementOptionsDialog(project, context)

      optionsDialog.show()

      return if (optionsDialog.exitCode == DialogWrapper.OK_EXIT_CODE) {
        optionsDialog.getUpdateStatementOptions()
      } else {
        null
      }
    }

    override fun generateStatement(
      sqlBuilder: SQLStatementBuilder,
      result: UpdateStatementOptions
    ): String {
      return sqlBuilder.buildUpdate(result)
    }

  }

}
