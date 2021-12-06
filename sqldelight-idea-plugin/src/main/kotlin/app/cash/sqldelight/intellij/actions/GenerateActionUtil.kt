package app.cash.sqldelight.intellij.actions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset

internal fun insertNewLineAndCleanup(editor: Editor, anchor: PsiElement) {
  val document = editor.document
  val s = "\n\n"
  document.insertString(anchor.endOffset, s)
  val startOffset = anchor.endOffset + s.length
  document.replaceString(startOffset, document.textLength, "")
  editor.caretModel.moveToOffset(startOffset)
  editor.scrollingModel.scrollToCaret(ScrollType.RELATIVE)
  editor.selectionModel.removeSelection()
}
