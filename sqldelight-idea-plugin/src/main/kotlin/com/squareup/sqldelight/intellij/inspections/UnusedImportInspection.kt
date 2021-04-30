package com.squareup.sqldelight.intellij.inspections

import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.squareup.sqldelight.core.lang.psi.ImportStmtMixin
import com.squareup.sqldelight.core.lang.util.findChildrenOfType
import com.squareup.sqldelight.core.psi.SqlDelightColumnType
import com.squareup.sqldelight.core.psi.SqlDelightJavaType
import com.squareup.sqldelight.core.psi.SqlDelightJavaTypeName

class UnusedImportInspection : LocalInspectionTool() {

  override fun runForWholeFile(): Boolean = true

  override fun checkFile(
    file: PsiFile,
    manager: InspectionManager,
    isOnTheFly: Boolean
  ): Array<ProblemDescriptor> {
    val javaTypes = file.columnJavaTypes()

    return file.findChildrenOfType<ImportStmtMixin>()
      .filter { importStmtMixin ->
        importStmtMixin.javaType.text.substringAfterLast(".").removeSuffix(";") !in javaTypes
      }
      .map { importStmtMixin ->
        manager.createProblemDescriptor(
          importStmtMixin, "Unused import", isOnTheFly,
          arrayOf(RemoveUnusedImportQuickFix(file)), ProblemHighlightType.LIKE_UNUSED_SYMBOL
        )
      }
      .toTypedArray()
  }

  class RemoveUnusedImportQuickFix(file: PsiFile) : LocalQuickFixOnPsiElement(file) {

    override fun getFamilyName(): String = name

    override fun getText(): String = "Optimize imports"

    override fun invoke(
      project: Project,
      file: PsiFile,
      startElement: PsiElement,
      endElement: PsiElement
    ) {
      OptimizeImportsProcessor(project, file).run()
    }
  }
}

fun PsiFile.columnJavaTypes(): Set<String> =
  findChildrenOfType<SqlDelightColumnType>()
    .asSequence()
    .flatMap { columnType ->
      columnType.findChildrenOfType<SqlDelightJavaType>() + columnType.findChildrenOfType<SqlDelightJavaTypeName>()
    }
    .mapNotNull { it.text.substringBefore(".") }
    .toSet()
