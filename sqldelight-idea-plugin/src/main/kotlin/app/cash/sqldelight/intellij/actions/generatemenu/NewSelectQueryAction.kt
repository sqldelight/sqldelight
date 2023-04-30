package app.cash.sqldelight.intellij.actions.generatemenu

import app.cash.sqldelight.intellij.actions.generatemenu.ui.SelectQueryOptionsDialog
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper

internal class NewSelectQueryAction : BaseSqlDelightGenerateAction(SelectHandler()) {

  class SelectHandler : BaseStatementGenerationHandler<SelectQueryOptions>() {

    override fun displayOptions(project: Project, context: StatementGenerationContext): SelectQueryOptions? {
      val optionsDialog = SelectQueryOptionsDialog(project, context)

      optionsDialog.show()

      return if (optionsDialog.exitCode == DialogWrapper.OK_EXIT_CODE) {
        optionsDialog.getSelectQueryOptions()
      } else {
        null
      }
    }

    override fun generateStatement(
      sqlBuilder: SQLStatementBuilder,
      result: SelectQueryOptions
    ): String {
      return sqlBuilder.buildSelect(result)
    }
  }

}

