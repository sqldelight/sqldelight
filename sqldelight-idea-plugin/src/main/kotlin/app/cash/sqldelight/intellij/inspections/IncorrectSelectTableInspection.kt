package app.cash.sqldelight.intellij.inspections

import app.cash.sqldelight.core.SqlDelightProjectService
import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.core.lang.util.findChildrenOfType
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.alecstrong.sql.psi.core.psi.SqlCreateViewStmt
import com.alecstrong.sql.psi.core.psi.SqlDeleteStmtLimited
import com.alecstrong.sql.psi.core.psi.SqlSelectStmt
import com.alecstrong.sql.psi.core.psi.SqlUpdateStmtLimited
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType

internal class IncorrectSelectTableInspection : LocalInspectionTool() {
  override fun checkFile(
    file: PsiFile,
    manager: InspectionManager,
    isOnTheFly: Boolean
  ): Array<ProblemDescriptor> {
    if (file !is SqlDelightFile) return emptyArray()

    val projectService = SqlDelightProjectService.getInstance(file.project)
    if (file.module?.let { module -> projectService.fileIndex(module).isConfigured } != true) {
      // Do not attempt to inspect the file types if the project is not configured yet.
      return emptyArray()
    }

    val tableNames = file.findChildrenOfType<SqlCreateTableStmt>()
      .map { it.name() }

    val viewNames = file.findChildrenOfType<SqlCreateViewStmt>()
      .map { it.name() }

    // No tables or views in your file, you can query from everything.
    if (tableNames.isEmpty()) { // only check table names
      return emptyArray()
    }

    val names = tableNames + viewNames
    val selects = file.findChildrenOfType<SqlSelectStmt>()

    return selects
      .asSequence()
      .filterNot { it.parentOfType<SqlSelectStmt>(withSelf = false) != null } // Ignore nested SELECT queries.
      .filterNot { it.parentOfType<SqlCreateViewStmt>(withSelf = false) != null } // Ignore SELECT when creating View.
      .filterNot { it.parentOfType<SqlDeleteStmtLimited>(withSelf = false) != null } // Ignore SELECT when Deleting.
      .filterNot { it.parentOfType<SqlUpdateStmtLimited>(withSelf = false) != null } // Ignore SELECT when Updating.
      .mapNotNull { selectStmt ->
        val tableNamesInQuery = selectStmt.joinClause?.tableOrSubqueryList?.mapNotNull { it.tableName?.name }
          .orEmpty()

        if (tableNamesInQuery.size == 1 && !names.contains(tableNamesInQuery.first())) {
          val from = tableNamesInQuery.first()
          manager.createProblemDescriptor(
            selectStmt,
            "Selecting from $from although that table isn't created in this file",
            isOnTheFly,
            emptyArray(),
            ProblemHighlightType.WARNING,
          )
        } else {
          null
        }
      }
      .toList()
      .toTypedArray()
  }
}
