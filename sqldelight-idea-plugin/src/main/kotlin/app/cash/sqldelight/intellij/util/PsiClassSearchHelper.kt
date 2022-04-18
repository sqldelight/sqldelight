package app.cash.sqldelight.intellij.util

import app.cash.sqldelight.intellij.util.PsiClassSearchHelper.ImportableType.JavaType
import app.cash.sqldelight.intellij.util.PsiClassSearchHelper.ImportableType.KotlinType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.stubindex.KotlinClassShortNameIndex
import org.jetbrains.kotlin.psi.KtClassOrObject
import javax.swing.Icon

internal object PsiClassSearchHelper {

  fun getClassesByShortName(shortName: String, project: Project, scope: GlobalSearchScope): List<ImportableType> {
    val kotlinClasses = KotlinClassShortNameIndex.getInstance()
      .get(shortName, project, scope)
      .map(::KotlinType)
      .sortedBy { it.qualifiedName }

    val javaClasses = PsiShortNamesCache.getInstance(project)
      .getClassesByName(shortName, scope)
      .map(::JavaType)
      .sortedBy { it.qualifiedName }

    return (kotlinClasses + javaClasses)
      .distinctBy { it.qualifiedName }
  }

  sealed interface ImportableType {
    val qualifiedName: String?
    val name: String?
    val innerClasses: Collection<ImportableType>

    fun getIcon(flags: Int): Icon?

    class KotlinType(val type: KtClassOrObject) : ImportableType {
      override val qualifiedName = type.fqName?.asString()
      override val name = type.name
      override val innerClasses = PsiTreeUtil.findChildrenOfType(type, KtClassOrObject::class.java).map(::KotlinType)

      override fun getIcon(flags: Int) = type.getIcon(flags)
    }

    class JavaType(val type: PsiClass) : ImportableType {
      override val qualifiedName = type.qualifiedName
      override val name = type.name
      override val innerClasses = type.innerClasses.map(::JavaType)

      override fun getIcon(flags: Int) = type.getIcon(flags)
    }
  }
}
