package app.cash.sqldelight.intellij

import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.core.lang.psi.ImportStmtMixin
import app.cash.sqldelight.core.lang.psi.JavaTypeMixin
import app.cash.sqldelight.core.lang.util.findChildrenOfType
import app.cash.sqldelight.core.psi.SqlDelightImportStmt
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelTypeAliasFqNameIndex
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance

internal class SqlDelightReferenceContributor : PsiReferenceContributor() {
  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
    registrar.registerReferenceProvider(
      psiElement(JavaTypeMixin::class.java),
      object : PsiReferenceProvider() {
        override fun getReferencesByElement(
          element: PsiElement,
          context: ProcessingContext,
        ): Array<PsiReference> {
          return arrayOf(JavaTypeReference(element as JavaTypeMixin))
        }
      },
    )
  }

  internal class JavaTypeReference(element: JavaTypeMixin) :
    PsiReferenceBase<JavaTypeMixin>(element, element.lastChild.textRangeInParent) {

    override fun handleElementRename(newElementName: String): PsiElement {
      return element.setName(newElementName)
    }

    override fun resolve(): PsiElement? {
      val elementText = element.text
      val qName = if (element.parent is SqlDelightImportStmt) {
        elementText
      } else {
        val prefix = elementText.substringBefore('.')
        val file = element.containingFile as SqlDelightFile
        val withImport = file.sqlStmtList
          ?.findChildrenOfType<ImportStmtMixin>()
          ?.firstOrNull { it.javaType.text.endsWith(prefix) }
          ?.javaType?.text?.plus(elementText.removePrefix(prefix))
        when {
          withImport != null -> withImport
          elementText.contains('.') -> elementText
          else -> typeForThisPackage(file)
        }
      }
      val project = element.project
      val scope = GlobalSearchScope.allScope(project)
      val classNameIndex = KotlinFullClassNameIndex.getInstance()
      val ktClass = { classNameIndex[qName, project, scope].firstOrNull() }
      val typeAliasFqNameIndex = getKotlinTopLevelTypeAliasFqNameIndex()
      val typeAlias = { typeAliasFqNameIndex[qName, project, scope].firstOrNull() }
      val javaPsiFacade = JavaPsiFacade.getInstance(project)
      val javaClass = { javaPsiFacade.findClass(qName, scope) }
      return ktClass() ?: typeAlias() ?: javaClass()
    }

    private fun typeForThisPackage(file: SqlDelightFile) = "${file.packageName}.${element.text}"
  }
}

private fun getKotlinTopLevelTypeAliasFqNameIndex(): KotlinTopLevelTypeAliasFqNameIndex {
  // read the INSTANCE variable reflectively first (newer Kotlin plugins)
  try {
    val instanceField = KotlinTopLevelTypeAliasFqNameIndex::class.java.getField("INSTANCE")
    val instance = instanceField.get(null)
    if (instance is KotlinTopLevelTypeAliasFqNameIndex) {
      return instance
    }
  } catch (e: Exception) {
    /* intentionally empty, fall back to getInstance() call in case of errors */
  }
  // Call the method getInstance on the companion type.
  val companionMethod =
    KotlinTopLevelTypeAliasFqNameIndex::class.companionObject!!.java.getMethod("getInstance")
  return companionMethod.invoke(KotlinTopLevelTypeAliasFqNameIndex::class.companionObjectInstance!!)
    as KotlinTopLevelTypeAliasFqNameIndex
}
