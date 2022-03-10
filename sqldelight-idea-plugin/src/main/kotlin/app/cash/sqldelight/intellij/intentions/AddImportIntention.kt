package app.cash.sqldelight.intellij.intentions

import app.cash.sqldelight.core.lang.psi.JavaTypeMixin
import app.cash.sqldelight.core.lang.util.findChildrenOfType
import app.cash.sqldelight.core.psi.SqlDelightImportStmt
import app.cash.sqldelight.intellij.util.PsiClassSearchHelper.ImportableType
import com.intellij.codeInsight.daemon.QuickFixBundle
import com.intellij.codeInsight.intention.BaseElementAtCaretIntentionAction
import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
import com.intellij.ui.popup.list.ListPopupImpl
import javax.swing.Icon

internal class AddImportIntention(
  private val classes: List<ImportableType>,
  private val isAvailable: Boolean,
) : BaseElementAtCaretIntentionAction() {

  override fun getFamilyName(): String = INTENTIONS_FAMILY_NAME_IMPORTS

  override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
    if (element.parentOfType<JavaTypeMixin>(withSelf = true) == null || element.context is SqlDelightImportStmt) return false
    text = "Add import for ${element.text}"
    return isAvailable
  }

  override fun invoke(project: Project, editor: Editor, element: PsiElement) {
    val document = editor.document
    val file = element.containingFile
    if (classes.size == 1) {
      document.addImport(file, "import ${classes.first().qualifiedName};")
    } else {
      showImportPopup(project, editor, file, classes)
    }
  }

  private fun showImportPopup(
    project: Project,
    editor: Editor,
    psiFile: PsiFile,
    classes: List<ImportableType>
  ) {
    val document = editor.document
    val step = object : BaseListPopupStep<ImportableType>(
      QuickFixBundle.message("class.to.import.chooser.title"), classes
    ) {
      override fun isAutoSelectionEnabled(): Boolean {
        return false
      }

      override fun getTextFor(value: ImportableType): String {
        return requireNotNull(value.qualifiedName)
      }

      override fun getIconFor(value: ImportableType): Icon? {
        return value.getIcon(Iconable.ICON_FLAG_VISIBILITY)
      }

      override fun onChosen(selectedValue: ImportableType?, finalChoice: Boolean): PopupStep<*>? {
        if (selectedValue == null) {
          return FINAL_CHOICE
        }
        if (finalChoice) {
          return doFinalStep {
            PsiDocumentManager.getInstance(project).commitAllDocuments()
            WriteCommandAction.runWriteCommandAction(
              project, QuickFixBundle.message("add.import"),
              null,
              {
                document.addImport(psiFile, "import ${selectedValue.qualifiedName};")
              }
            )
          }
        }
        return super.onChosen(selectedValue, finalChoice)
      }
    }
    val popup = ListPopupImpl(project, step)
    NavigationUtil.hidePopupIfDumbModeStarts(popup, project)
    popup.showInBestPositionFor(editor)
  }

  private fun Document.addImport(file: PsiFile, import: String) {
    val imports = file.findChildrenOfType<SqlDelightImportStmt>()
    if (imports.isEmpty()) {
      insertString(0, "$import\n\n")
    } else {
      val newImports = mutableListOf(import)
      var endOffset = 0
      for (imp in imports) {
        newImports.add(imp.text)
        endOffset = maxOf(endOffset, imp.textOffset + imp.textLength)
      }
      replaceString(0, endOffset, newImports.sorted().joinToString("\n"))
    }
  }
}
