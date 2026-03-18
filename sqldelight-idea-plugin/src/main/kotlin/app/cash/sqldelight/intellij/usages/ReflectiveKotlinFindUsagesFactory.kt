package app.cash.sqldelight.intellij.usages

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.usageView.UsageInfo
import java.lang.reflect.Method

class ReflectiveKotlinFindUsagesFactory private constructor(
  private val wrapped: FindUsagesHandlerFactory,
) {
  constructor(project: Project) : this(createKotlinFindUsagesHandlerFactory(project))

  private val findFunctionOptionsMethod: Method = wrapped.javaClass.getMethod("getFindFunctionOptions")
  private val findPropertyOptionsMethod: Method = wrapped.javaClass.getMethod("getFindPropertyOptions")

  val findFunctionOptions get() = findFunctionOptionsMethod.invoke(wrapped) as FindUsagesOptions
  val findPropertyOptions get() = findPropertyOptionsMethod.invoke(wrapped) as FindUsagesOptions

  fun createFindUsagesHandler(element: PsiElement, forHighlightUsages: Boolean): FindUsagesHandler {
    return wrapped.createFindUsagesHandler(element, forHighlightUsages)!!
  }

  fun isKotlinReferenceUsageInfo(info: UsageInfo): Boolean {
    return kotlinReferenceUsageInfoClass.isAssignableFrom(info.javaClass)
  }

  companion object {
    // IC 2023.1 or later
    private const val KOTLIN_USAGE_PACKAGE = "org.jetbrains.kotlin.idea.base.searching.usages"

    // older than IC 2023.1
    private const val LEGACY_KOTLIN_USAGE_PACKAGE = "org.jetbrains.kotlin.idea.findUsages"

    private val factoryClass = try {
      Class.forName("$KOTLIN_USAGE_PACKAGE.KotlinFindUsagesHandlerFactory")
    } catch (e: ClassNotFoundException) {
      // fall back to older version
      Class.forName("$LEGACY_KOTLIN_USAGE_PACKAGE.KotlinFindUsagesHandlerFactory")
    }

    private val kotlinReferenceUsageInfoClass = try {
      Class.forName("$KOTLIN_USAGE_PACKAGE.KotlinReferenceUsageInfo")
    } catch (e: ClassNotFoundException) {
      // fall back to older version
      Class.forName("$LEGACY_KOTLIN_USAGE_PACKAGE.KotlinReferenceUsageInfo")
    }

    private fun createKotlinFindUsagesHandlerFactory(project: Project): FindUsagesHandlerFactory {
      val ctor = factoryClass.getConstructor(Project::class.java)
      return ctor.newInstance(project) as FindUsagesHandlerFactory
    }
  }
}
