/*
 * Copyright (C) 2017 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.squareup.sqldelight.intellij.lang

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.lang.folding.NamedFoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.intellij.util.childOfType
import com.squareup.sqldelight.intellij.util.childrenForRule
import com.squareup.sqldelight.intellij.util.nextLeafOrNull
import org.antlr.intellij.adaptor.lexer.RuleElementType

class SqliteFoldingBuilder : FoldingBuilderEx(), DumbAware {

  override fun buildFoldRegions(
      root: PsiElement,
      document: Document,
      quick: Boolean
  ): Array<FoldingDescriptor> {
    val descriptors = mutableListOf<FoldingDescriptor>()
    val importElements = mutableListOf<PsiElement>()
    val createTableElements = mutableListOf<PsiElement>()
    val statementElements = mutableListOf<PsiElement>()
    root.collectElements(importElements, createTableElements, statementElements)
    descriptors += importElements.toImportDescriptors()
    descriptors += createTableElements.toCreateTableDescriptors()
    descriptors += statementElements.toStatementDescriptors()
    return descriptors.toTypedArray()
  }

  private fun PsiElement.collectElements(
      importElements: MutableList<PsiElement>,
      createElements: MutableList<PsiElement>,
      statementElements: MutableList<PsiElement>
  ) {
    importElements += childrenForRule(SqliteParser.RULE_import_stmt)
    createElements += childrenForRule(SqliteParser.RULE_create_table_stmt)
    statementElements += childrenForRule(SqliteParser.RULE_sql_stmt)
    children.forEach { it.collectElements(importElements, createElements, statementElements) }
  }

  private fun List<PsiElement>.toImportDescriptors(): List<FoldingDescriptor> {

    fun toDescriptor(): FoldingDescriptor? {
      val whitespaceElement = first().childOfType<PsiWhiteSpace>() ?: return null
      val start = whitespaceElement.textRange.endOffset
      val end = last().textRange.endOffset
      return NamedFoldingDescriptor(first(), start, end, null, "...")
    }

    return when {
      size > 1 -> listOf(toDescriptor()).filterNotNull()
      else -> emptyList()
    }
  }

  private fun List<PsiElement>.toCreateTableDescriptors(): List<FoldingDescriptor> {

    fun PsiElement.toDescriptor(): FoldingDescriptor? {
      val openingBraceElement = firstChild?.nextLeafOrNull { text == "(" } ?: return null
      val closingSemicolonElement = nextSibling ?: return null
      val start = openingBraceElement.textRange.startOffset
      val end = closingSemicolonElement.textRange.endOffset
      return NamedFoldingDescriptor(this, start, end, null, "(...);")
    }

    return map { it.toDescriptor() }.filterNotNull().toList()
  }

  private fun List<PsiElement>.toStatementDescriptors(): List<FoldingDescriptor> {

    fun PsiElement.toDescriptor(): FoldingDescriptor? {
      val openingColonElement = firstChild?.nextLeafOrNull { text == ":" } ?: return null
      val closingSemicolonElement = nextSibling ?: return null
      val start = openingColonElement.textRange.endOffset
      val end = closingSemicolonElement.textRange.endOffset
      return NamedFoldingDescriptor(this, start, end, null, "...")
    }

    return map { it.toDescriptor() }.filterNotNull().toList()
  }

  override fun getPlaceholderText(node: ASTNode) = "..."

  override fun isCollapsedByDefault(node: ASTNode) = with(node.elementType) {
    this is RuleElementType && ruleIndex == SqliteParser.RULE_import_stmt
  }
}
