package app.cash.sqldelight.intellij

import app.cash.sqldelight.core.SqlDelightFileIndex
import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.core.lang.psi.StmtIdentifierMixin
import app.cash.sqldelight.core.lang.queriesName
import app.cash.sqldelight.core.psi.SqlDelightStmtIdentifier
import app.cash.sqldelight.core.psi.SqlDelightStmtList
import app.cash.sqldelight.intellij.usages.ReflectiveKotlinFindUsagesFactory
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.getValueParameters

internal class SqlDelightFindUsagesHandlerFactory : FindUsagesHandlerFactory() {
  override fun canFindUsages(element: PsiElement): Boolean {
    val module = ModuleUtil.findModuleForPsiElement(element) ?: return false
    val isConfigured = SqlDelightFileIndex.getInstance(module).isConfigured
    return (element is SqlDelightStmtIdentifier || element is SqlColumnName) && isConfigured
  }

  override fun createFindUsagesHandler(
    element: PsiElement,
    forHighlightUsages: Boolean,
  ): FindUsagesHandler? {
    return try {
      SqlDelightIdentifierHandler(element)
    } catch (e: ProcessCanceledException) {
      null
    }
  }
}

internal class SqlDelightIdentifierHandler(
  private val element: PsiElement,
) : FindUsagesHandler(element) {
  private val factory = ReflectiveKotlinFindUsagesFactory(element.project)
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
    options: FindUsagesOptions,
  ): Boolean {
    val generatedFiles = element.generatedVirtualFiles()
    val ignoringFileProcessor = Processor<UsageInfo> { t ->
      if (factory.isKotlinReferenceUsageInfo(t) && t.virtualFile in generatedFiles) {
        return@Processor true
      }
      processor.process(t)
    }
    super.processElementUsages(element, ignoringFileProcessor, options)
    return kotlinHandlers.all {
      ApplicationManager.getApplication().runReadAction(
        Computable<Boolean> {
          it.processElementUsages(
            it.primaryElements.single(),
            ignoringFileProcessor,
            kotlinFindUsagesOptions,
          )
        },
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

internal fun PsiElement.generatedVirtualFiles(): List<VirtualFile> = ReadAction.compute<List<VirtualFile>, Throwable> {
  if (!isValid) return@compute emptyList()

  val module = ModuleUtil.findModuleForPsiElement(this) ?: return@compute emptyList()
  val fileIndex = SqlDelightFileIndex.getInstance(module)
  val generatedDirectories = (containingFile as SqlDelightFile).generatedDirectories.orEmpty()
    .mapNotNull { fileIndex.contentRoot.findFileByRelativePath(it) }
  val rootManager = ProjectRootManager.getInstance(project)
  generatedDirectories.flatMap { dir ->
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
  val identifierList = parentOfType<SqlDelightStmtList>()?.stmtIdentifierList.orEmpty()
  val namedStmts = identifierList.associateBy { it.getNextSiblingIgnoringWhitespaceAndComments() as? SqlStmt }
  val stmtsWithColumn = namedStmts.keys.filterNotNull()
    .filter { stmt ->
      PsiTreeUtil.findChildrenOfType(stmt, SqlColumnName::class.java).any { it.textMatches(name) }
    }

  val generatedMethodParameters = stmtsWithColumn.mapNotNull { namedStmts[it] as? StmtIdentifierMixin }
    .flatMap { it.generatedMethods() }
    .flatMap { it.getValueParameters() }
    .filter { it.name == name }

  return generatedMethodParameters + generatedKtFiles()
    .flatMap { it.declarations.filterIsInstance<KtClass>() }
    .filter(KtClass::isData)
    .flatMap(KtClass::getPrimaryConstructorParameters)
    .filter { it.name == name }
    .toList()
}
