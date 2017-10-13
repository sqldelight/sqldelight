package com.squareup.sqldelight.core;

import com.alecstrong.sqlite.psi.core.SqliteAnnotationHolder
import com.google.common.truth.Truth.assertWithMessage
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.File
import java.util.ArrayList

@RunWith(Parameterized::class)
class ErrorTest(val fixtureRoot: File, val name: String) {
  @Test fun execute() {
    val parser = TestEnvironment()
    val errors = ArrayList<String>()

    val environment = parser.build(fixtureRoot.path, object : SqliteAnnotationHolder {
      override fun createErrorAnnotation(element: PsiElement, s: String?) {
        val documentManager = PsiDocumentManager.getInstance(element.project)
        val name = element.containingFile.name
        val document = documentManager.getDocument(element.containingFile)!!
        val lineNum = document.getLineNumber(element.textOffset)
        val offsetInLine = element.textOffset - document.getLineStartOffset(lineNum)
        errors.add("$name line ${lineNum+1}:$offsetInLine - $s")
      }
    })

    val sourceFiles = StringBuilder()
    environment.forSourceFiles {
      sourceFiles.append("${it.name}:\n")
      it.printTree {
        sourceFiles.append("  ")
        sourceFiles.append(it)
      }
    }

    val expectedFailure = File(fixtureRoot, "failure.txt")
    if (expectedFailure.exists()) {
      assertWithMessage(sourceFiles.toString()).that(errors).containsExactlyElementsIn(
          expectedFailure.readText().split("\n").filterNot { it.isEmpty() }
      )
    } else {
      assertWithMessage(sourceFiles.toString()).that(errors).isEmpty()
    }
  }

  fun PsiElement.printTree(printer: (String) -> Unit) {
    printer("$this\n")
    children.forEach { child ->
      child.printTree { printer("  $it") }
    }
  }

  companion object {
    @Suppress("unused") // Used by Parameterized JUnit runner reflectively.
    @Parameters(name = "{1}")
    @JvmStatic fun parameters() = File("src/test/errors").listFiles()
        .filter { it.isDirectory }
        .map { arrayOf(it, it.name) }
  }
}
