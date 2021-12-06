package com.squareup.sqldelight.intellij.intentions

import app.cash.sqldelight.core.lang.util.findChildrenOfType
import com.intellij.codeInsight.daemon.QuickFixBundle
import com.intellij.codeInsight.intention.BaseElementAtCaretIntentionAction
import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.popup.list.ListPopupImpl
import com.squareup.sqldelight.core.psi.SqlDelightImportStmt
import com.squareup.sqldelight.intellij.util.PsiClassSearchHelper
import javax.swing.Icon

class AddImportIntention(private val key: String) : BaseElementAtCaretIntentionAction() {
  override fun getFamilyName(): String = INTENTIONS_FAMILY_NAME_IMPORTS

  override fun getText(): String = "Add import for $key"

  override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
    return true
  }

  override fun invoke(project: Project, editor: Editor, element: PsiElement) {
    val module = ModuleUtil.findModuleForPsiElement(element) ?: return
    val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
    val classes = PsiClassSearchHelper.getClassesByShortName(key, project, scope)
    val document = editor.document
    val file = element.containingFile
    if (classes.size == 1) {
      document.addImport(file, "import ${classes.first().qualifiedName};")
    } else {
      showImportPopup(project, editor, file, classes.sortedBy { it.qualifiedName })
    }
  }

  private fun showImportPopup(
    project: Project,
    editor: Editor,
    psiFile: PsiFile,
    classes: List<PsiClass>
  ) {
    val document = editor.document
    val step = object : BaseListPopupStep<PsiClass>(
      QuickFixBundle.message("class.to.import.chooser.title"), classes
    ) {
      override fun isAutoSelectionEnabled(): Boolean {
        return false
      }

      override fun getTextFor(value: PsiClass): String {
        return requireNotNull(value.qualifiedName)
      }

      override fun getIconFor(value: PsiClass): Icon? {
        return value.getIcon(Iconable.ICON_FLAG_VISIBILITY)
      }

      override fun onChosen(selectedValue: PsiClass?, finalChoice: Boolean): PopupStep<*>? {
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
