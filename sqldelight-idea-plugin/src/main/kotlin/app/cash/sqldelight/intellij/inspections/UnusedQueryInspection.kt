package app.cash.sqldelight.intellij.inspections

import app.cash.sqldelight.core.lang.psi.StmtIdentifierMixin
import app.cash.sqldelight.core.lang.util.findChildOfType
import app.cash.sqldelight.core.psi.SqlDelightStmtIdentifier
import app.cash.sqldelight.core.psi.SqlDelightVisitor
import app.cash.sqldelight.intellij.SqlDelightFindUsagesHandlerFactory
import com.alecstrong.sql.psi.core.psi.SqlStmtList
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.application.ReadActionProcessor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.ReadOnlyModificationException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usageView.UsageInfo
import com.intellij.util.CommonProcessors

internal class UnusedQueryInspection : LocalInspectionTool() {

  private val findUsagesFactory = SqlDelightFindUsagesHandlerFactory()
  override fun buildVisitor(
    holder: ProblemsHolder,
    isOnTheFly: Boolean,
    session: LocalInspectionToolSession,
  ): PsiElementVisitor {
    return ensureReady(session.file) {
      object : SqlDelightVisitor() {
        override fun visitStmtIdentifier(o: SqlDelightStmtIdentifier) {
          ignoreInvalidElements {
            if (o !is StmtIdentifierMixin || o.identifier() == null) {
              return
            }
            if (!hasUsages(o)) {
              holder.registerProblem(o, "Unused symbol", ProblemHighlightType.LIKE_UNUSED_SYMBOL, SafeDeleteQuickFix(o))
            }
          }
        }
      }
    }
  }

  private fun hasUsages(stmtIdentifier: SqlDelightStmtIdentifier): Boolean {
    if (!findUsagesFactory.canFindUsages(stmtIdentifier)) {
      return false
    }
    val findFirstProcessor = CommonProcessors.FindFirstProcessor<UsageInfo>()
    val readActionProcessor = ReadActionProcessor.wrapInReadAction(findFirstProcessor)
    val findUsagesOptions = FindUsagesOptions(stmtIdentifier.project).apply {
      isUsages = true
      isSearchForTextOccurrences = false
    }
    val findUsagesHandler = findUsagesFactory.createFindUsagesHandler(stmtIdentifier, true)
    findUsagesHandler?.processElementUsages(stmtIdentifier, readActionProcessor, findUsagesOptions)
    return findFirstProcessor.isFound
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
      endElement: PsiElement,
    ) {
      WriteCommandAction.writeCommandAction(project).run<ReadOnlyModificationException> {
        val element = ref.element ?: return@run
        val semicolon = PsiTreeUtil.findSiblingForward(element, SqlTypes.SEMI, false, null)
        file.findChildOfType<SqlStmtList>()?.deleteChildRange(element, semicolon)
      }
    }
  }
}
