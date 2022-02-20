package app.cash.sqldelight.intellij

import app.cash.sqldelight.core.SqlDelightFileIndex
import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.core.lang.psi.ImportStmtMixin
import app.cash.sqldelight.core.lang.psi.JavaTypeMixin
import app.cash.sqldelight.core.lang.util.findChildrenOfType
import app.cash.sqldelight.core.psi.SqlDelightImportStmt
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelTypeAliasFqNameIndex
import org.jetbrains.kotlin.psi.psiUtil.endOffset

internal class SqlDelightReferenceContributor : PsiReferenceContributor() {
  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
    registrar.registerReferenceProvider(
      psiElement(JavaTypeMixin::class.java),
      object : PsiReferenceProvider() {
        override fun getReferencesByElement(
          element: PsiElement,
          context: ProcessingContext
        ): Array<PsiReference> {
          return arrayOf(JavaTypeReference(element as JavaTypeMixin))
        }
      }
    )
  }

  internal class JavaTypeReference(element: JavaTypeMixin) :
    PsiReferenceBase<JavaTypeMixin>(element, TextRange(0, element.endOffset)) {

    override fun resolve(): PsiElement? {
      val module = ModuleUtilCore.findModuleForPsiElement(element)
        ?: return null

      val elementText = element.text
      val qName = if (element.parent is SqlDelightImportStmt) {
        elementText
      } else {
        val prefix = elementText.substringBefore('.')
        val file = element.containingFile as SqlDelightFile
        file.sqlStmtList
          ?.findChildrenOfType<ImportStmtMixin>()
          ?.firstOrNull { it.javaType.text.endsWith(prefix) }
          ?.javaType?.text?.plus(elementText.removePrefix(prefix)) ?: typeForThisPackage(
          module,
          file
        )
      }
      val project = element.project
      val scope = module.getModuleWithDependenciesAndLibrariesScope(false)
      val classNameIndex = KotlinFullClassNameIndex.getInstance()
      val ktClass = { classNameIndex[qName, project, scope].firstOrNull() }
      val typeAliasFqNameIndex = KotlinTopLevelTypeAliasFqNameIndex.getInstance()
      val typeAlias = { typeAliasFqNameIndex[qName, project, scope].firstOrNull() }
      val javaPsiFacade = JavaPsiFacade.getInstance(project)
      val javaClass = { javaPsiFacade.findClass(qName, scope) }
      return ktClass() ?: typeAlias() ?: javaClass()
    }

    private fun typeForThisPackage(
      module: Module,
      file: SqlDelightFile
    ) = "${SqlDelightFileIndex.getInstance(module).packageName(file)}.${element.text}"
  }
}
