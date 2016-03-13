package com.squareup.sqldelight.intellij.util

import com.android.tools.idea.gradle.parser.GradleBuildFile
import com.android.tools.idea.gradle.util.GradleUtil
import com.intellij.openapi.module.Module
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement

fun iterateClasspath(
    module: Module,
    classpathIterator: GradleBuildFile.(GroovyPsiElement) -> Unit
): GradleBuildFile? {
  val file = GradleUtil.getGradleBuildFile(module);
  return if (file == null) null else object : GradleBuildFile(file, module.project) {
    override fun onPsiFileAvailable() {
      for (method in getMethodCalls(getClosure("buildscript/dependencies") ?: return,
          "classpath")) {
        val literal = getFirstArgument(method)
        if (literal != null) {
          classpathIterator(literal)
        }
      }
    }
  }
}
