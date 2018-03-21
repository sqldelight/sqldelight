package com.squareup.sqldelight.intellij.lang

import com.alecstrong.sqlite.psi.core.SqliteAnnotationHolder
import com.alecstrong.sqlite.psi.core.psi.SqliteAnnotatedElement
import com.intellij.lang.Language
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.FileViewProviderFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.util.PsiTreeUtil
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.SqlDelightProjectService
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.SqlDelightLanguage
import com.squareup.sqldelight.intellij.util.GeneratedVirtualFile
import java.io.PrintStream

class SqlDelightFileViewProviderFactory : FileViewProviderFactory {
  override fun createFileViewProvider(
    file: VirtualFile,
    language: Language,
    manager: PsiManager,
    eventSystemEnabled: Boolean
  ): FileViewProvider {
    val module = SqlDelightProjectService.getInstance(manager.project).module(file)
    if (module == null || !SqlDelightFileIndex.getInstance(module).isConfigured) {
      return SingleRootFileViewProvider(manager, file, eventSystemEnabled)
    }
    return SqlDelightFileViewProvider(manager, file, eventSystemEnabled, language, module)
  }
}

private class SqlDelightFileViewProvider(
  manager: PsiManager,
  virtualFile: VirtualFile,
  eventSystemEnabled: Boolean,
  language: Language,
  private val module: Module
) : SingleRootFileViewProvider(manager, virtualFile, eventSystemEnabled, language) {
  var currentFiles = emptyList<String>()
  var previousFiles = emptyList<String>()

  val file: SqlDelightFile
    get() = getPsiInner(SqlDelightLanguage) as SqlDelightFile

  override fun contentsSynchronized() {
    super.contentsSynchronized()

    var shouldGenerate = true
    val annotationHolder = object : SqliteAnnotationHolder {
      override fun createErrorAnnotation(element: PsiElement, s: String) {
        shouldGenerate = false
      }

    }
    shouldGenerate = PsiTreeUtil.processElements(file) { element ->
      when (element) {
        is PsiErrorElement -> return@processElements false
        is SqliteAnnotatedElement -> element.annotate(annotationHolder)
      }
      return@processElements shouldGenerate
    }

    if (shouldGenerate) {
      previousFiles = currentFiles
      val files = mutableListOf<String>()
      SqlDelightCompiler.compile(module, file) { filePath ->
        files.add(filePath)
        val vFile: VirtualFile by GeneratedVirtualFile(filePath)
        PrintStream(vFile.getOutputStream(this))
      }
      currentFiles = files
      (previousFiles - currentFiles).forEach { filePath ->
        val vFile: VirtualFile by GeneratedVirtualFile(filePath)
        vFile.delete(this)
      }
    }
  }
}