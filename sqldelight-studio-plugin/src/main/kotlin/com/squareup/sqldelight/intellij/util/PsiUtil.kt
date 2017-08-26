/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.sqldelight.intellij.util

import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.squareup.sqldelight.SqliteParser
import org.antlr.intellij.adaptor.lexer.RuleElementType
import org.antlr.intellij.adaptor.lexer.TokenElementType
import org.antlr.v4.runtime.ParserRuleContext

internal inline fun <reified R : PsiElement> PsiElement.parentOfType(): R? =
    PsiTreeUtil.getParentOfType(this, R::class.java)

internal inline fun <reified R : PsiElement> PsiElement.childOfType(): R? =
    PsiTreeUtil.getChildOfType(this, R::class.java)

internal fun PsiElement.getDeepestFirst() = PsiTreeUtil.getDeepestFirst(this)

internal fun PsiFile.collectElements(predicate: (element: PsiElement) -> Boolean) =
    PsiTreeUtil.collectElements(this, predicate)

internal fun PsiFile.processElements(processor: (element: PsiElement) -> Boolean) =
    PsiTreeUtil.processElements(this, processor)

internal val PsiElement.elementType: IElementType
    get() = node.elementType

internal fun PsiElement.childrenForRule(rule: Int) = children.filter {
      when (it.elementType) {
        is TokenElementType -> (it.elementType as TokenElementType).type == rule
        is RuleElementType -> (it.elementType as RuleElementType).ruleIndex == rule
        else -> false
      }
    }

fun PsiDirectory.getOrCreateSubdirectory(name: String) = findSubdirectory(name) ?: createSubdirectory(name)

fun PsiDirectory.getOrCreateFile(name: String) = findFile(name) ?: createFile(name)

fun SqliteParser.ParseContext.elementAt(offset: Int): ParserRuleContext? {
  if (sql_stmt_list() == null) return null
  for (i in 0 until sql_stmt_list().childCount) {
    val child = sql_stmt_list().getChild(i) as? ParserRuleContext ?: continue
    if (child.start.startIndex < offset && child.stop.stopIndex > offset) {
      return child
    }
  }
  return null
}
