package app.cash.sqldelight.intellij

import app.cash.sqldelight.core.lang.SqlDelightFile
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager

internal class ActiveEditorChangeListener : FileEditorManagerListener {

  override fun selectionChanged(event: FileEditorManagerEvent) {
    val editor = (event.oldEditor as TextEditor?)?.editor ?: return
    val project = editor.project ?: return
    val file = (PsiDocumentManager.getInstance(project).getPsiFile(editor.document) as? SqlDelightFile?)?.virtualFile ?: return

    PsiManager.getInstance(project).findViewProvider(file)?.contentsSynchronized()
  }
}
