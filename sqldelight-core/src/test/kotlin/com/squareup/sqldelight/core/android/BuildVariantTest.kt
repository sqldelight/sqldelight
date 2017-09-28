package com.squareup.sqldelight.core.android

import com.alecstrong.sqlite.psi.core.SqliteAnnotationHolder
import com.alecstrong.sqlite.psi.core.SqliteCoreEnvironment
import com.google.common.truth.Truth
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.squareup.sqldelight.core.lang.SqlDelightFileType
import com.squareup.sqldelight.core.lang.SqlDelightParserDefinition
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.File
import java.util.ArrayList

@RunWith(Parameterized::class)
class BuildVariantTest(val fixtureRoot: File, val name: String) {
  private val parserDefinition = SqlDelightParserDefinition()

  @Test
  fun execute() {
    val errors = ArrayList<String>()
    val environment = SqliteCoreEnvironment(parserDefinition, SqlDelightFileType, fixtureRoot.path)
    environment.annotate(object : SqliteAnnotationHolder {
      override fun createErrorAnnotation(element: PsiElement, s: String?) {
        val documentManager = PsiDocumentManager.getInstance(element.project)
        val name = element.containingFile.name
        val document = documentManager.getDocument(element.containingFile)!!
        val lineNum = document.getLineNumber(element.textOffset)
        val offsetInLine = element.textOffset - document.getLineStartOffset(lineNum)
        errors.add("$name line ${lineNum + 1}:$offsetInLine - $s")
      }
    })

    val expectedFailure = File(fixtureRoot, "failure.txt")
    if (expectedFailure.exists()) {
      Truth.assertThat(errors).containsExactlyElementsIn(
          expectedFailure.readText().split("\n").filterNot { it.isEmpty() }
      )
    } else {
      Truth.assertThat(errors).isEmpty()
    }
  }

  companion object {
    @Suppress("unused") // Used by Parameterized JUnit runner reflectively.
    @Parameters(name = "{1}")
    @JvmStatic fun parameters(): List<Array<Any>> =
        File("src/test/build-variant-fixtures").listFiles()
            .filter { it.isDirectory }
            .map { arrayOf(it, it.name) }
  }
}
