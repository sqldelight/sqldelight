/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.sqldelight.generating

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.squareup.sqldelight.SqliteCompiler
import com.squareup.sqldelight.SqliteCompiler.Status
import com.squareup.sqldelight.lang.SqliteFile

internal class SqlDocumentAnnotator : ExternalAnnotator<Pair<SqliteFile, TableGenerator>, SqlDocumentAnnotator.Generation>() {
  private val localFileSystem = LocalFileSystem.getInstance()
  private val sqliteCompiler = SqliteCompiler<PsiElement>()

  override fun collectInformation(file: PsiFile) = Pair(file as SqliteFile, TableGenerator.create(file))

  override fun doAnnotate(pair: Pair<SqliteFile, TableGenerator>): Generation? {
    val tableGenerator = pair.second
    val status = sqliteCompiler.write(tableGenerator)
    val file = localFileSystem.findFileByIoFile(tableGenerator.outputDirectory)
    file?.refresh(false, true)
    val generatedFile = file?.findFileByRelativePath(
        tableGenerator.packageDirectory + "/" + tableGenerator.generatedFileName + ".java")
    if (generatedFile != pair.first.generatedFile?.virtualFile) {
      WriteCommandAction.runWriteCommandAction(pair.first.project, { pair.first.generatedFile?.delete() })
    }
    return Generation(status, generatedFile)
  }

  override fun apply(file: PsiFile, generation: Generation, holder: AnnotationHolder) {
    when (generation.status.result) {
      SqliteCompiler.Status.Result.FAILURE -> holder.createErrorAnnotation(
          generation.status.originatingElement,
          generation.status.errorMessage)
      SqliteCompiler.Status.Result.SUCCESS -> {
        if (generation.generatedFile == null) return
        val document = FileDocumentManager.getInstance().getDocument(generation.generatedFile)
        document?.createGuardedBlock(0, document.textLength)
        (file as SqliteFile).generatedFile = file.getManager().findFile(generation.generatedFile)
      }
    }
  }

  class Generation internal constructor(val status: Status<PsiElement>, val generatedFile: VirtualFile?)
}
