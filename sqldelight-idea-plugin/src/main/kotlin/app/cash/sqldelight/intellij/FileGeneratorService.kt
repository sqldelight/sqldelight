package app.cash.sqldelight.intellij

import app.cash.sqldelight.core.SqlDelightFileIndex
import app.cash.sqldelight.core.compiler.SqlDelightCompiler
import app.cash.sqldelight.core.lang.MigrationFile
import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.core.lang.SqlDelightQueriesFile
import app.cash.sqldelight.intellij.util.GeneratedVirtualFile
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlAnnotatedElement
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import java.io.PrintStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

interface FileGeneratorService {
  fun generateFiles(file: SqlDelightFile)

  companion object {
    fun getInstance(project: Project): FileGeneratorService {
      return project.getService(FileGeneratorService::class.java)
    }
  }
}

class FileGeneratorServiceImpl : FileGeneratorService {

  private val threadPool = Executors.newScheduledThreadPool(1)

  private var filesGenerated = emptyList<VirtualFile>()
    set(value) {
      (field - value).forEach { it.delete(this) }
      field = value
    }

  private var condition = WriteCondition()

  override fun generateFiles(file: SqlDelightFile) {
    val module = ModuleUtil.findModuleForFile(file) ?: return
    if (!SqlDelightFileIndex.getInstance(module).isConfigured ||
      SqlDelightFileIndex.getInstance(module).sourceFolders(file).isEmpty()
    ) {
      return
    }

    condition.invalidated.set(true)

    if (ApplicationManager.getApplication().isUnitTestMode) {
      generateSqlDelightCode(file)
      return
    }

    val thisCondition = WriteCondition()
    condition = thisCondition
    threadPool.schedule(
      {
        ApplicationManager.getApplication().invokeLater(
          {
            try {
              generateSqlDelightCode(file)
            } catch (e: Throwable) {
              // IDE generating code should be best effort - source of truth is always the gradle
              // build, and its better to ignore the error and try again than crash and require
              // the IDE restarts.
              e.printStackTrace()
            }
          },
          thisCondition
        )
      },
      1, TimeUnit.SECONDS
    )
  }

  /**
   * Attempt to generate the SQLDelight code for the file represented by the view provider.
   */
  private fun generateSqlDelightCode(file: SqlDelightFile) {
    val module = ModuleUtil.findModuleForFile(file) ?: return
    var shouldGenerate = true
    val annotationHolder = object : SqlAnnotationHolder {
      override fun createErrorAnnotation(element: PsiElement, s: String) {
        shouldGenerate = false
      }
    }

    // File is mutable so create a copy that wont be mutated.
    val file = file.copy() as SqlDelightFile

    shouldGenerate = try {
      PsiTreeUtil.processElements(file) { element ->
        when (element) {
          is PsiErrorElement -> return@processElements false
          is SqlAnnotatedElement -> element.annotate(annotationHolder)
        }
        return@processElements shouldGenerate
      }
    } catch (e: Throwable) {
      // If we encountered an exception while looking for errors, assume it was an error.
      false
    }

    if (shouldGenerate && !ApplicationManager.getApplication().isUnitTestMode) ApplicationManager.getApplication().runWriteAction {
      val files = mutableListOf<VirtualFile>()
      val fileAppender = { filePath: String ->
        val vFile: VirtualFile by GeneratedVirtualFile(filePath, module)
        files.add(vFile)
        PrintStream(vFile.getOutputStream(this))
      }
      if (file is SqlDelightQueriesFile) {
        SqlDelightCompiler.writeInterfaces(module, file, fileAppender)
      } else if (file is MigrationFile) {
        SqlDelightCompiler.writeInterfaces(file, fileAppender)
      }
      this.filesGenerated = files
    }
  }

  private class WriteCondition : Condition<Any> {
    var invalidated = AtomicBoolean(false)

    override fun value(t: Any?) = invalidated.get()
  }
}
