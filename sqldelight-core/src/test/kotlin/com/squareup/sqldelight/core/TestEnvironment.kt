package com.squareup.sqldelight.core

import com.alecstrong.sqlite.psi.core.SqliteAnnotationHolder
import com.alecstrong.sqlite.psi.core.SqliteCoreEnvironment
import com.intellij.psi.PsiElement
import java.io.File

internal class TestEnvironment {
  fun build(root: String): SqliteCoreEnvironment {
    return build(root, object : SqliteAnnotationHolder {
      override fun createErrorAnnotation(element: PsiElement, s: String) {
        throw IllegalStateException(s)
      }
    })
  }

  fun build(root: String, annotationHolder: SqliteAnnotationHolder): SqlDelightEnvironment {
    val environment = SqlDelightEnvironment(listOf(File(root)), "com.example", File("out"))
    environment.annotate(annotationHolder)
    return environment
  }
}