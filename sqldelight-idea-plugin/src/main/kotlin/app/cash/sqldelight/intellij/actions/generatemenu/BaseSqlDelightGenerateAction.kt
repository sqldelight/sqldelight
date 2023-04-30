package app.cash.sqldelight.intellij.actions.generatemenu

import app.cash.sqldelight.core.lang.SqlDelightQueriesFile
import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.generation.actions.BaseGenerateAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

/**
 * A base class for IDE code generation actions. Implements [#isValidForFile] to limit the actions
 * to SQLDelight queries files.
 */
open class BaseSqlDelightGenerateAction(handler: CodeInsightActionHandler) : BaseGenerateAction(handler) {

    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
        return file is SqlDelightQueriesFile
    }

}