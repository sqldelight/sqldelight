package com.squareup.sqldelight.intellij

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.module.ModuleUtil
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
    val module = ModuleUtil.findModuleForPsiElement(element)
    return module != null && element is SqlDelightStmtIdentifier &&
      SqlDelightFileIndex.getInstance(module).isConfigured
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
    processor: Processor<in UsageInfo>,
    options: FindUsagesOptions
  ): Boolean {
    val ignoringFileProcessor = Processor<UsageInfo> { t ->
      if (t is KotlinReferenceUsageInfo && t.virtualFile in element.generatedFiles()) {
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

internal fun PsiElement.generatedFiles(): List<VirtualFile> {
  val paths = (containingFile as SqlDelightFile).let { file ->
    file.generatedDirectories?.map { "$it/${file.virtualFile?.queriesName}.kt" }
  } ?: return emptyList()
  val module = ModuleUtil.findModuleForPsiElement(this) ?: return emptyList()
  return paths.mapNotNull {
    SqlDelightFileIndex.getInstance(module).contentRoot.findFileByRelativePath(it)
  }
}

internal fun StmtIdentifierMixin.generatedMethods(): Collection<KtNamedDeclaration> {
  val files = generatedFiles().map {
    PsiManager.getInstance(project).findFile(it) as KtFile
  }
  return files.flatMap { file ->
    PsiTreeUtil.findChildrenOfType(file, KtNamedFunction::class.java).filter {
      it.name == identifier()?.text
    }
  }
}
