package com.squareup.sqldelight.intellij.inspections

import com.alecstrong.sql.psi.core.psi.SqlStmtList
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.ReadOnlyModificationException
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.psi.StmtIdentifierMixin
import com.squareup.sqldelight.core.lang.queriesName
import com.squareup.sqldelight.core.lang.util.findChildOfType
import com.squareup.sqldelight.core.lang.util.findChildrenOfType
import com.squareup.sqldelight.core.psi.SqlDelightStmtIdentifier
import com.squareup.sqldelight.core.psi.SqlDelightVisitor
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.psi.KtNamedFunction

class UnusedQueryInspection : LocalInspectionTool() {

  override fun buildVisitor(
    holder: ProblemsHolder,
    isOnTheFly: Boolean,
    session: LocalInspectionToolSession
  ): PsiElementVisitor {
    val sqlDelightFile = session.file as SqlDelightFile
    val module =
      ModuleUtil.findModuleForPsiElement(sqlDelightFile) ?: return PsiElementVisitor.EMPTY_VISITOR
    val fileName = "${sqlDelightFile.virtualFile?.queriesName}.kt"
    val generatedFile = FilenameIndex.getFilesByName(
      sqlDelightFile.project, fileName, GlobalSearchScope.moduleScope(module)
    ).firstOrNull() ?: return PsiElementVisitor.EMPTY_VISITOR
    val allMethods = generatedFile.findChildrenOfType<KtNamedFunction>()
    return object : SqlDelightVisitor() {
      override fun visitStmtIdentifier(o: SqlDelightStmtIdentifier) {
        if (o !is StmtIdentifierMixin || o.identifier() == null) {
          return
        }
        val generatedMethods = allMethods.filter { namedFunction ->
          namedFunction.name == o.identifier()?.text
        }
        for (generatedMethod in generatedMethods) {
          val lightMethods = generatedMethod.toLightMethods()
          for (psiMethod in lightMethods) {
            val reference = ReferencesSearch.search(psiMethod, psiMethod.useScope).findFirst()
            if (reference != null) {
              return
            }
          }
        }
        holder.registerProblem(
          o, "Unused symbol", ProblemHighlightType.LIKE_UNUSED_SYMBOL, SafeDeleteQuickFix(o)
        )
      }
    }
  }

  class SafeDeleteQuickFix(element: PsiElement) : LocalQuickFixOnPsiElement(element) {
    private val ref = SmartPointerManager.getInstance(element.project)
      .createSmartPsiElementPointer(element, element.containingFile)

    override fun getFamilyName(): String = name

    override fun getText(): String = "Safe delete ${ref.element?.text?.removeSuffix(":").orEmpty()}"

    override fun invoke(
      project: Project,
      file: PsiFile,
      startElement: PsiElement,
      endElement: PsiElement
    ) {
      WriteCommandAction.writeCommandAction(project).run<ReadOnlyModificationException> {
        val element = ref.element ?: return@run
        val semicolon = PsiTreeUtil.findSiblingForward(element, SqlTypes.SEMI, false, null)
        file.findChildOfType<SqlStmtList>()?.deleteChildRange(element, semicolon)
      }
    }
  }
}
