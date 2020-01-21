package com.squareup.sqldelight.test.util

import com.alecstrong.sqlite.psi.core.SqliteAnnotationHolder
import com.alecstrong.sqlite.psi.core.SqliteCoreEnvironment
import com.intellij.psi.PsiElement
import com.squareup.sqldelight.core.SqlDelightDatabaseProperties
import com.squareup.sqldelight.core.SqlDelightEnvironment
import java.io.File

internal class TestEnvironment(private val outputDirectory: File = File("output")) {
  fun build(root: String): SqliteCoreEnvironment {
    return build(root, object : SqliteAnnotationHolder {
      override fun createErrorAnnotation(element: PsiElement, s: String) {
        throw IllegalStateException(s)
      }
    })
  }

  fun build(root: String, annotationHolder: SqliteAnnotationHolder): SqlDelightEnvironment {
    val environment = SqlDelightEnvironment(
        sourceFolders = listOf(File(root)),
        dependencyFolders = emptyList(),
        properties = SqlDelightDatabaseProperties(
            packageName = "com.example",
            className = "TestDatabase",
            dependencies = emptyList(),
            compilationUnits = emptyList(),
            outputDirectory = outputDirectory
        ),
        outputDirectory = outputDirectory,
        moduleName = "testmodule"
    )
    environment.annotate(annotationHolder)
    return environment
  }
}