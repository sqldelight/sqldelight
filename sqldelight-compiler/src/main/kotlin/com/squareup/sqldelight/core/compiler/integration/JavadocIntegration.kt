package com.squareup.sqldelight.core.compiler.integration

import com.intellij.psi.PsiElement

/**
 * This pattern consists of 3 parts:
 *
 * - `/\\*\\*` - matches the first line of the Javadoc:
 *
 * ```
 * </**>
 *  * Javadoc
 *  */
 * ```
 *
 * - `\n \\*[ /]?` - matches every other line of Javadoc:
 *
 * ```
 * /**<
 *  * >Javadoc<
 *  */>
 * ```
 *
 * - ` \\**slash` - specifically matches the tail part of a single-line Javadoc:
 *
 * ```
 * /* Javadoc< */>
 * ```
 */
private val JAVADOC_TEXT_REGEX = Regex("/\\*\\*|\n \\*[ /]?| \\*/")

internal fun javadocText(javadoc: PsiElement): String? {
  return javadoc.text
      .split(JAVADOC_TEXT_REGEX)
      .dropWhile(String::isEmpty)
      .joinToString(separator = "\n", transform = String::trim)
}
