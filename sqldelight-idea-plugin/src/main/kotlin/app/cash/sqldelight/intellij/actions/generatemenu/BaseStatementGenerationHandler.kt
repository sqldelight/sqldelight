package app.cash.sqldelight.intellij.actions.generatemenu

import app.cash.sqldelight.core.lang.util.findChildOfType
import app.cash.sqldelight.core.psi.SqlDelightStmtList
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import kotlin.math.max

/**
 * A base class for statement generation [CodeInsightActionHandler]s. Handles most of the
 * interaction with the IDE.
 * 
 * @param T the class hold the results of the options UI
 */
abstract class BaseStatementGenerationHandler<T> : CodeInsightActionHandler {

  /**
   * Display the options selection UI. If the user cancels, this method returns null, otherwise it
   * returns the options selected.
   */
  protected abstract fun displayOptions(project: Project, context: StatementGenerationContext): T?

  /**
   * Generate the SQL statement String for insertion into the file, using the results from the
   * options UI.
   */
  protected abstract fun generateStatement(sqlBuilder: SQLStatementBuilder, result: T): String

  override fun startInWriteAction(): Boolean {
    return false
  }

  override fun invoke(project: Project, editor: Editor, file: PsiFile) {
    val stmtList = file.findChildOfType<SqlDelightStmtList>() ?: return

    val createTableStatements = stmtList.stmtList.mapNotNull { it.createTableStmt }

    if (createTableStatements.isEmpty()) return

    val offset = editor.caretModel.offset
    val activeCreateStatement = file.findElementAt(offset)?.parentOfType<SqlCreateTableStmt>()

    val context = StatementGenerationContext(createTableStatements, activeCreateStatement)

    val result = displayOptions(project, context)

    if (result != null) {
      runWriteAction {
        // prep document for statement insertion
        val startOffset = padAndGetStatementStartOffset(file, editor)
        val document = editor.document

        val statementSQLBuilder = SQLStatementBuilder(createTableStatements)
        val statement = generateStatement(statementSQLBuilder, result)
        document.insertString(startOffset, statement)

        insertAfterStatementPadding(startOffset + statement.length, editor)
      }
    }
  }

  /**
   * Figure out where to place the generated statement and add newlines as needed.
   *
   * Returns the offset at which to insert the statement
   */
  private fun padAndGetStatementStartOffset(
    file: PsiFile,
    editor: Editor
  ): Int {
    val caretOffset = editor.caretModel.offset
    val anchor = file.findElementAt(caretOffset)?.parentOfType<SqlStmt>()

    val startOffset = if (anchor != null) {
      anchor.endOffset + 1
    } else {
      caretOffset
    }
    val document = editor.document
    val s = if (anchor != null) {
      "\n\n"
    } else {
      // we're outside a statement, so figure out spacing to apply
      val precedingText = document.getText(TextRange(max(0, startOffset - 2), startOffset))
      if (precedingText.length < 2) {
        // start of file
        ""
      } else {
        if (precedingText.endsWith(";")) {
          "\n\n"
        } else if (precedingText == "\n\n") {
          ""
        } else {
          "\n"
        }
      }
    }
    document.insertString(startOffset, s)

    return startOffset + s.length
  }

  /**
   * Inserts newlines after the inserted statement as needed to provide a blank line between
   * statements.
   */
  private fun insertAfterStatementPadding(
    startOffset: Int,
    editor: Editor
  ) {
    val document = editor.document
    // we're outside a statement, so figure out spacing to apply
    val afterText = document.getText(TextRange(startOffset, startOffset + 2))
    val padding = if (afterText.length < 2) {
      // end of file
      ""
    } else {
      if (afterText == "\n\n") {
        ""
      } else if (afterText.startsWith("\n")) {
        "\n"
      } else  {
        "\n\n"
      }
    }
    document.insertString(startOffset, padding)
  }

}
