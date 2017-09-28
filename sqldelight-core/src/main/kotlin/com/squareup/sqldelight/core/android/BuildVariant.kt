package com.squareup.sqldelight.core.android

import com.intellij.psi.PsiDirectory
import java.util.ArrayList

class BuildVariant(root: PsiDirectory): PsiDirectory by root {
  internal val name = root.name

  /**
   * Returns an ordered list of the build variants that this build variant will combine into the
   * apps source. The list is ordered by precedence, with this build variant overriding any of the
   * source from the returned build variants.
   */
  fun parentVariants(): List<BuildVariant> {
    if (name == "main") return emptyList()

    val result = ArrayList<BuildVariant>()
    val indexOfBuildType = name.indexOfFirst { it.isUpperCase() }
    if (indexOfBuildType != -1) {
      parentDirectory?.findSubdirectory(name.substring(indexOfBuildType).decapitalize())?.let {
        result.add(BuildVariant(it))
      }
      parentDirectory?.findSubdirectory(name.substring(0, indexOfBuildType))?.let {
        result.add(BuildVariant(it))
      }
    }
    parentDirectory?.findSubdirectory("main")?.let {
      result.add(BuildVariant(it))
    }

    return result
  }
}
