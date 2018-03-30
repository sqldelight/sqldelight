package com.squareup.sqldelight.intellij

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.psi.StmtIdentifierMixin
import com.squareup.sqldelight.core.lang.queriesName
import com.squareup.sqldelight.core.psi.SqlDelightStmtIdentifier
import org.jetbrains.kotlin.idea.findUsages.KotlinFindUsagesHandlerFactory
import org.jetbrains.kotlin.idea.findUsages.KotlinReferenceUsageInfo
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction

class SqlDelightFindUsagesHandlerFactory : FindUsagesHandlerFactory() {
  override fun canFindUsages(element: PsiElement): Boolean {
    return element.module() != null && element is SqlDelightStmtIdentifier &&
        SqlDelightFileIndex.getInstance(element.module()!!).isConfigured
  }

  override fun createFindUsagesHandler(
    element: PsiElement,
    forHighlightUsages: Boolean
  ): FindUsagesHandler = SqlDelightIdentifierHandler(element as StmtIdentifierMixin)
}

private class SqlDelightIdentifierHandler(
  private val element: StmtIdentifierMixin
) : FindUsagesHandler(element) {
  private val factory = KotlinFindUsagesHandlerFactory(element.project)
  private val kotlinHandlers = element.generatedMethods().map {
    factory.createFindUsagesHandler(it, false)
  }

  override fun getPrimaryElements() = arrayOf(element)

  override fun processElementUsages(
    sourceElement: PsiElement,
    processor: Processor<UsageInfo>,
    options: FindUsagesOptions
  ): Boolean {
    val ignoringFileProcessor = Processor<UsageInfo> { t ->
      if (t is KotlinReferenceUsageInfo && t.virtualFile == element.generatedFile()) {
        return@Processor true
      }
      processor.process(t)
    }
    return kotlinHandlers.all {
      it.processElementUsages(
          it.primaryElements.single(),
          ignoringFileProcessor,
          factory.findFunctionOptions
      )
    }
  }
}

internal fun PsiElement.generatedFile(): VirtualFile? {
  val path = (containingFile as SqlDelightFile).let { file ->
    "${file.generatedDir}/${file.virtualFile?.queriesName}.kt"
  }
  val module = module() ?: return null
  return SqlDelightFileIndex.getInstance(module).contentRoot.findFileByRelativePath(path)
}

internal fun StmtIdentifierMixin.generatedMethods(): Collection<KtNamedDeclaration> {
  val generatedQueries = generatedFile() ?: return emptyList()
  val file = PsiManager.getInstance(project).findFile(generatedQueries) as KtFile
  return PsiTreeUtil.findChildrenOfType(file, KtNamedFunction::class.java).filter {
    it.name == identifier()?.text
  }
}