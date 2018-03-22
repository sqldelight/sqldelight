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

import com.alecstrong.sqlite.psi.core.psi.SqliteTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.lang.folding.NamedFoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.squareup.sqldelight.core.SqlDelightTypes
import com.squareup.sqldelight.core.psi.SqlDelightStmtIdentifier
import com.squareup.sqldelight.intellij.util.childOfType
import com.squareup.sqldelight.intellij.util.prevSiblingOfType

class SqlDelightFoldingBuilder : FoldingBuilder, DumbAware {

  override fun buildFoldRegions(root: ASTNode, document: Document) =
      root.createFoldingDescriptors()

  private fun ASTNode.createFoldingDescriptors(): Array<FoldingDescriptor> {
    val descriptors = mutableListOf<FoldingDescriptor>()
    val importElements = mutableListOf<PsiElement>()
    getChildren(null)
        .filter { it.elementType == SqlDelightTypes.SQL_STMT_LIST }
        .flatMap { it.getChildren(null).toList() }
        .forEach { statement ->
          when (statement.elementType) {
            SqlDelightTypes.IMPORT_STMT -> importElements += statement.psi
            SqliteTypes.STATEMENT -> {
              val psi = statement.psi
              val sqlStatement = statement.firstChildNode?.firstChildNode
              val descriptor = if (sqlStatement?.elementType == SqliteTypes.CREATE_TABLE_STMT) {
                psi.toCreateTableDescriptor(sqlStatement?.psi)
              } else {
                psi.toStatementDescriptor(psi.prevSiblingOfType<SqlDelightStmtIdentifier>())
              }
              if (descriptor != null) {
                descriptors += descriptor
              }
            }
          }
        }
    descriptors += importElements.toImportDescriptors()
    return descriptors.toTypedArray()
  }

  private fun PsiElement.toCreateTableDescriptor(createTableStmt: PsiElement?): FoldingDescriptor? {
    val openingBraceElement = createTableStmt?.node?.findChildByType(SqliteTypes.LP) ?: return null
    val start = openingBraceElement.textRange.startOffset
    val end = lastChild.textRange.endOffset
    return NamedFoldingDescriptor(this, start, end, null, "(...);")
  }

  private fun PsiElement.toStatementDescriptor(stmtIdentifier: PsiElement?): FoldingDescriptor? {
    if (stmtIdentifier == null) return null
    val start = stmtIdentifier.textRange.endOffset
    val end = lastChild.textRange.endOffset
    return NamedFoldingDescriptor(this, start, end, null, "...")
  }

  private fun List<PsiElement>.toImportDescriptors(): List<FoldingDescriptor> {

    fun toDescriptor(): FoldingDescriptor? {
      val whitespaceElement = first().childOfType<PsiWhiteSpace>() ?: return null
      val start = whitespaceElement.textRange.endOffset
      val end = last().textRange.endOffset
      return NamedFoldingDescriptor(first(), start, end, null, "...")
    }

    return if (size > 1) listOfNotNull(toDescriptor()) else emptyList()
  }

  override fun getPlaceholderText(node: ASTNode) = "..."

  override fun isCollapsedByDefault(node: ASTNode) = false
}
