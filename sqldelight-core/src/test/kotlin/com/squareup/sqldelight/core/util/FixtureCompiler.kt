/*
 * Copyright (C) 2017 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.squareup.sqldelight.core.util

import com.alecstrong.sqlite.psi.core.SqliteAnnotationHolder
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.squareup.sqldelight.core.TestEnvironment
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler
import com.squareup.sqldelight.core.lang.SqlDelightFile
import org.junit.rules.TemporaryFolder
import java.io.File

private typealias CompilationMethod = (SqlDelightFile, (String) -> Appendable) -> Unit

object FixtureCompiler {

  fun compileSql(
      sql: String,
      temporaryFolder: TemporaryFolder,
      compilationMethod: CompilationMethod = SqlDelightCompiler::compile
  ): CompilationResult {
    val srcRootDir = temporaryFolder.newFolder("src")
    val fixtureRootDir = File(srcRootDir, "test/test-fixture").apply { mkdirs() }
    val fixtureSrcDir = File(fixtureRootDir, "src/main/sqldelight").apply { mkdirs() }
    File(fixtureSrcDir, "Test.sq").apply {
      createNewFile()
      writeText(sql)
    }
    return compileFixture(fixtureRootDir.path, compilationMethod)
  }

  fun compileFixture(
      fixtureRoot: String,
      compilationMethod: CompilationMethod = SqlDelightCompiler::compile
  ): CompilationResult {
    val compilerOutput = mutableMapOf<File, StringBuilder>()
    val errors = mutableListOf<String>()
    val sourceFiles = StringBuilder()
    val parser = TestEnvironment()
    val fixtureRootDir = File(fixtureRoot)
    if (!fixtureRootDir.exists()) {
      throw IllegalArgumentException("$fixtureRoot does not exist")
    }
    val environment = parser.build(fixtureRootDir.path, createAnnotationHolder(errors))
    environment.forSourceFiles { psiFile ->
      psiFile.log(sourceFiles)
      compilationMethod(psiFile as SqlDelightFile) { fileName ->
        val builder = StringBuilder()
        compilerOutput += File(fixtureRootDir, fileName) to builder
        return@compilationMethod builder
      }
    }
    return CompilationResult(fixtureRootDir, compilerOutput, errors, sourceFiles.toString())
  }

  private fun createAnnotationHolder(
      errors: MutableList<String>
  ) = object : SqliteAnnotationHolder {
    override fun createErrorAnnotation(element: PsiElement, s: String) {
      val documentManager = PsiDocumentManager.getInstance(element.project)
      val name = element.containingFile.name
      val document = documentManager.getDocument(element.containingFile)!!
      val lineNum = document.getLineNumber(element.textOffset)
      val offsetInLine = element.textOffset - document.getLineStartOffset(lineNum)
      errors += "$name line ${lineNum + 1}:$offsetInLine - $s"
    }
  }

  private fun PsiFile.log(sourceFiles: StringBuilder) {
    sourceFiles.append("$name:\n")
    printTree {
      sourceFiles.append("  ")
      sourceFiles.append(this)
    }
  }

  private fun PsiElement.printTree(printer: (String) -> Unit) {
    printer("$this\n")
    children.forEach { child ->
      child.printTree { printer("  $it") }
    }
  }

  data class CompilationResult(
      val fixtureRootDir: File,
      val compilerOutput: Map<File, StringBuilder>,
      val errors: List<String>,
      val sourceFiles: String
  )
}
