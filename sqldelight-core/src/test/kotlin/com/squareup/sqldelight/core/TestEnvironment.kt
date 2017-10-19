package com.squareup.sqldelight.core

import com.alecstrong.sqlite.psi.core.SqliteAnnotationHolder
import com.alecstrong.sqlite.psi.core.SqliteCoreEnvironment
import com.intellij.psi.PsiElement
import com.squareup.sqldelight.core.lang.SqlDelightFileType
import com.squareup.sqldelight.core.lang.SqlDelightParserDefinition

internal class TestEnvironment {
  private val parserDefinition = SqlDelightParserDefinition()

  fun build(root: String): SqliteCoreEnvironment {
    return build(root, object : SqliteAnnotationHolder {
      override fun createErrorAnnotation(element: PsiElement, s: String?) {
        throw IllegalStateException(s)
      }
    })
  }

  fun build(root: String, annotationHolder: SqliteAnnotationHolder): SqliteCoreEnvironment {
    val environment = SqliteCoreEnvironment(parserDefinition, SqlDelightFileType, root)
    environment.annotate(annotationHolder)
    return environment
  }
}