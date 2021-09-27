package com.squareup.sqldelight.intellij

import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.roots.ProjectRootManager
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
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction

class SqlDelightFindUsagesHandlerFactory : FindUsagesHandlerFactory() {
  override fun canFindUsages(element: PsiElement): Boolean {
    val module = ModuleUtil.findModuleForPsiElement(element) ?: return false
    val isConfigured = SqlDelightFileIndex.getInstance(module).isConfigured
    return (element is SqlDelightStmtIdentifier || element is SqlColumnName) && isConfigured
  }

  override fun createFindUsagesHandler(
    element: PsiElement,
    forHighlightUsages: Boolean
  ): FindUsagesHandler = SqlDelightIdentifierHandler(element)
}

private class SqlDelightIdentifierHandler(
  private val element: PsiElement
) : FindUsagesHandler(element) {
  private val factory = KotlinFindUsagesHandlerFactory(element.project)
  private val kotlinHandlers = when (element) {
    is StmtIdentifierMixin -> element.generatedMethods()
    is SqlColumnName -> element.generatedProperties()
    else -> throw AssertionError()
  }
    .map { factory.createFindUsagesHandler(it, false) }

  private val kotlinFindUsagesOptions: FindUsagesOptions = when (element) {
    is StmtIdentifierMixin -> factory.findFunctionOptions
    is SqlColumnName -> factory.findPropertyOptions
    else -> throw AssertionError()
  }

  override fun getPrimaryElements() = arrayOf(element)

  override fun processElementUsages(
    sourceElement: PsiElement,
    processor: Processor<in UsageInfo>,
    options: FindUsagesOptions
  ): Boolean {
    val generatedFiles = element.generatedVirtualFiles()
    val ignoringFileProcessor = Processor<UsageInfo> { t ->
      if (t is KotlinReferenceUsageInfo && t.virtualFile in generatedFiles) {
        return@Processor true
      }
      processor.process(t)
    }
    super.processElementUsages(element, ignoringFileProcessor, options)
    return kotlinHandlers.all {
      it.processElementUsages(
        it.primaryElements.single(),
        ignoringFileProcessor,
        kotlinFindUsagesOptions
      )
    }
  }
}

internal fun PsiElement.generatedQueryFiles(): List<VirtualFile> {
  val queriesName = (containingFile as SqlDelightFile).let { file ->
    file.virtualFile?.queriesName
  }
  return generatedVirtualFiles().filter { it.nameWithoutExtension == queriesName }
}

internal fun StmtIdentifierMixin.generatedMethods(): Collection<KtNamedDeclaration> {
  val files = generatedQueryFiles().map {
    PsiManager.getInstance(project).findFile(it) as KtFile
  }
  return files.flatMap { file ->
    PsiTreeUtil.findChildrenOfType(file, KtNamedFunction::class.java).filter {
      it.name == identifier()?.text
    }
  }
}

internal fun PsiElement.generatedVirtualFiles(): List<VirtualFile> {
  val module = ModuleUtil.findModuleForPsiElement(this) ?: return emptyList()
  val fileIndex = SqlDelightFileIndex.getInstance(module)
  val generatedDirectories = (containingFile as SqlDelightFile).generatedDirectories.orEmpty()
    .mapNotNull { fileIndex.contentRoot.findFileByRelativePath(it) }
  val rootManager = ProjectRootManager.getInstance(project)
  return generatedDirectories.flatMap { dir ->
    val mut = mutableListOf<VirtualFile>()
    rootManager.fileIndex.iterateContentUnderDirectory(dir) { file ->
      mut += file
      true
    }
    mut.toList()
  }
}

internal fun PsiElement.generatedKtFiles(): List<KtFile> {
  val psiManager = PsiManager.getInstance(project)
  return generatedVirtualFiles().mapNotNull { psiManager.findFile(it) }
    .filterIsInstance<KtFile>()
}

internal fun SqlColumnName.generatedProperties(): Collection<KtNamedDeclaration> {
  return generatedKtFiles().asSequence()
    .flatMap { it.declarations.filterIsInstance<KtClass>() }
    .filter(KtClass::isData)
    .flatMap(KtClass::getPrimaryConstructorParameters)
    .filter { it.name == name }
    .toList()
}
