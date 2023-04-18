package app.cash.sqldelight.intellij.intentions

import app.cash.sqldelight.core.lang.psi.StmtIdentifier
import com.alecstrong.sql.psi.core.psi.SqlCompoundSelectStmt
import com.alecstrong.sql.psi.core.psi.SqlCreateViewStmt
import com.alecstrong.sql.psi.core.psi.SqlStmtList
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.codeInsight.intention.BaseElementAtCaretIntentionAction
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.TextExpression
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.startOffset

internal class CreateViewIntention : BaseElementAtCaretIntentionAction() {

  override fun getFamilyName(): String {
    return INTENTIONS_FAMILY_NAME_REFACTORINGS
  }

  override fun getText(): String {
    return "Create view"
  }

  override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
    val selectStmt = element.parentOfType<SqlCompoundSelectStmt>(true)
    return selectStmt != null && selectStmt.parentOfType<SqlCreateViewStmt>() == null
  }

  override fun invoke(project: Project, editor: Editor, element: PsiElement) {
    PsiDocumentManager.getInstance(project).commitAllDocuments()

    val selectStmt = element.parentOfType<SqlCompoundSelectStmt>(true) ?: return
    val stmtList = selectStmt.parentOfType<SqlStmtList>() ?: return
    val container = stmtList.stmtList.firstOrNull {
      PsiTreeUtil.isAncestor(it, selectStmt, true)
    } ?: return

    val semi = PsiTreeUtil.findSiblingForward(container, SqlTypes.SEMI, false, null) ?: return
    val psiElement = container.getPrevSiblingIgnoringWhitespaceAndComments()

    val containerStart = if (psiElement is StmtIdentifier) {
      psiElement.startOffset
    } else {
      container.startOffset
    }
    val containerEnd = semi.endOffset
    val text = editor.document.getDocumentTextFragment(containerStart, containerEnd)
    val offset = selectStmt.startOffset - containerStart

    val templateManager = TemplateManager.getInstance(project)

    WriteCommandAction.runWriteCommandAction(project) {
      editor.document.deleteString(containerStart, containerEnd)

      val template = templateManager.createTemplate("", "")
      val expression = TextExpression("some_view")

      template.addTextSegment("CREATE VIEW ")
      template.addVariable("NAME", expression, true)
      template.addTextSegment(" AS ${selectStmt.text};\n\n")

      template.addTextSegment(text.substring(0, offset))
      template.addTextSegment("SELECT * FROM ")
      template.addVariableSegment("NAME")
      template.addTextSegment(text.substring(selectStmt.endOffset - containerStart))
      templateManager.startTemplate(editor, template)
    }
  }

  private fun Document.getDocumentTextFragment(startOffset: Int, endOffset: Int): String {
    return charsSequence.subSequence(startOffset, endOffset).toString()
  }
}
