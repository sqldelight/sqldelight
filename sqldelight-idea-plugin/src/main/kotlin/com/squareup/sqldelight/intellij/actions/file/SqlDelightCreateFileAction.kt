package com.squareup.sqldelight.intellij.actions.file

import app.cash.sqldelight.core.SqlDelightFileIndex
import app.cash.sqldelight.core.lang.SqlDelightFileType
import app.cash.sqldelight.core.lang.SqlDelightLanguage
import com.alecstrong.sql.psi.core.psi.SqlTableName
import com.alecstrong.sql.psi.core.psi.SqlTypes.TABLE
import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.actions.AttributesDefaults
import com.intellij.ide.fileTemplates.ui.CreateFromTemplateDialog
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.intellij.SqlDelightFileIconProvider
import org.jetbrains.kotlin.idea.util.findModule
import java.util.Properties

/**
 * Creates a new SqlDelight file/table/migration from a template (see [fileTemplates.internal])
 *
 * Adopted from [NewKotlinFileAction](https://github.com/JetBrains/kotlin/blob/f6a739bbc5b66a8f2cb5cb61acb1e87c899fa11e/idea/src/org/jetbrains/kotlin/idea/actions/NewKotlinFileAction.kt#L56)
 */
class SqlDelightCreateFileAction :
  CreateFileFromTemplateAction(
    CAPTION,
    "Creates new SqlDelight file or table",
    SqlDelightFileType.icon
  ),
  DumbAware {

  override fun postProcess(
    createdElement: PsiFile,
    templateName: String,
    customProperties: MutableMap<String, String>?
  ) {
    super.postProcess(createdElement, templateName, customProperties)

    if (createdElement is SqlDelightFile) {
      val isTableTemplate = PsiTreeUtil.collectElements(createdElement) { it is SqlTableName }.isNotEmpty()
      val editor = FileEditorManager.getInstance(createdElement.project).selectedTextEditor ?: return
      if (editor.document == createdElement.viewProvider.document) {
        if (isTableTemplate) {
          // move caret inside "create table" sql statement
          val lineCount = editor.document.lineCount
          if (lineCount > 0) {
            editor.caretModel.moveToLogicalPosition(LogicalPosition(lineCount - 2, 0))
          }
        } else {
          // move caret after "create table", so the user can enter the table name conveniently
          val tableKeyword = PsiTreeUtil.collectElements(createdElement) { it.node.elementType == TABLE }.firstOrNull() ?: return
          editor.caretModel.moveToOffset(tableKeyword.textRange.endOffset + 1)
        }
      }
    }
  }

  override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?): String = CAPTION

  override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
    val icon = SqlDelightFileIconProvider.getIcon(SqlDelightLanguage, project)
    builder.setTitle("New $CAPTION")
      .addKind("File", icon, "SqlDelight File")
      .addKind("Table", icon, "SqlDelight Table")
      .addKind("Migration", icon, "SqlDelight Migration")
      .setValidator(NameValidator)
  }

  override fun isAvailable(dataContext: DataContext): Boolean {
    if (!super.isAvailable(dataContext)) return false

    val project = CommonDataKeys.PROJECT.getData(dataContext) ?: return false
    val file = CommonDataKeys.VIRTUAL_FILE.getData(dataContext) ?: return false

    val module = file.findModule(project) ?: return false

    return SqlDelightFileIndex.getInstance(module).isConfigured
  }

  override fun startInWriteAction() = false

  override fun createFileFromTemplate(name: String, template: FileTemplate, dir: PsiDirectory): PsiFile? {
    val (className, targetDir) = findOrCreateTarget(dir, name, template.extension)
    return createFromTemplate(targetDir, className, template)
  }

  companion object {
    private const val CAPTION = "SqlDelight File/Table"

    private val FQNAME_SEPARATORS = charArrayOf('/', '\\', '.')

    private object NameValidator : InputValidatorEx {
      private val numberRegex = Regex(".*\\d.*")

      override fun getErrorText(inputString: String): String? {
        if (inputString.trim().isEmpty()) {
          return "Name can't be empty"
        }

        val parts: List<String> = inputString.split(*FQNAME_SEPARATORS)
        if (parts.any { it.trim().isEmpty() }) {
          return "Name can't have empty parts"
        }

        if (parts.any { it.contains("migration") || it.contains("sqm") } &&
          parts.none { it.matches(numberRegex) }
        ) {
          return "Migration filenames must contain a number"
        }

        return null
      }

      override fun checkInput(inputString: String): Boolean = true

      override fun canClose(inputString: String): Boolean = getErrorText(inputString) == null
    }

    private fun findOrCreateTarget(dir: PsiDirectory, name: String, extension: String): Pair<String, PsiDirectory> {
      var className = name.removeSuffix(".$extension")
      var targetDir = dir

      for (splitChar in FQNAME_SEPARATORS) {
        if (splitChar in className) {
          val names = className.trim().split(splitChar)

          for (dirName in names.dropLast(1)) {
            targetDir = targetDir.findSubdirectory(dirName) ?: runWriteAction {
              targetDir.createSubdirectory(dirName)
            }
          }

          className = names.last()
          break
        }
      }
      return className to targetDir
    }

    private fun createFromTemplate(dir: PsiDirectory, className: String, template: FileTemplate): PsiFile? {
      val project = dir.project
      val defaultProperties = FileTemplateManager.getInstance(project).defaultProperties

      val properties = Properties(defaultProperties)

      val element = try {
        CreateFromTemplateDialog(
          project, dir, template,
          AttributesDefaults(className).withFixedName(true),
          properties
        ).create()
      } catch (e: IncorrectOperationException) {
        throw e
      } catch (e: Exception) {
        LOG.error(e)
        return null
      }

      return element?.containingFile
    }
  }
}
