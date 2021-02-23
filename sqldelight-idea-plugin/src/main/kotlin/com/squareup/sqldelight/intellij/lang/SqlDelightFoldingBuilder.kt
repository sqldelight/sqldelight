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

import com.alecstrong.sql.psi.core.psi.SqlCreateIndexStmt
import com.alecstrong.sql.psi.core.psi.SqlCreateTriggerStmt
import com.alecstrong.sql.psi.core.psi.SqlCreateViewStmt
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.squareup.sqldelight.core.psi.SqlDelightStmtIdentifier
import com.squareup.sqldelight.core.psi.SqldelightTypes
import com.squareup.sqldelight.intellij.util.prevSiblingOfType
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

class SqlDelightFoldingBuilder : FoldingBuilder, DumbAware {

  override fun buildFoldRegions(root: ASTNode, document: Document) =
    root.createFoldingDescriptors()

  private fun ASTNode.createFoldingDescriptors(): Array<FoldingDescriptor> {
    return getChildren(null)
      .filter { it.elementType == SqldelightTypes.STMT_LIST }
      .flatMap {
        val descriptors = mutableListOf<FoldingDescriptor>()
        val statements = it.getChildren(null).toList()
        for (statement in statements) {
          when (statement.elementType) {
            SqldelightTypes.IMPORT_STMT_LIST ->
              statement.psi.toImportListDescriptor()?.let(descriptors::add)
            SqlTypes.STMT -> {
              val psi = statement.psi
              val sqlStatement = statement.firstChildNode
              when (sqlStatement?.elementType) {
                SqlTypes.CREATE_TABLE_STMT ->
                  psi.toCreateTableDescriptor(sqlStatement?.psi)?.let(descriptors::add)
                SqlTypes.CREATE_VIEW_STMT ->
                  psi.toCreateViewDescriptor(sqlStatement?.psi)?.let(descriptors::add)
                SqlTypes.CREATE_TRIGGER_STMT ->
                  psi.toCreateTriggerDescriptor(sqlStatement?.psi)?.let(descriptors::add)
                SqlTypes.CREATE_INDEX_STMT ->
                  psi.toCreateIndexDescriptor(sqlStatement?.psi)?.let(descriptors::add)
              }
              val stmtIdentifier = psi.prevSiblingOfType<SqlDelightStmtIdentifier>()
              if (stmtIdentifier?.identifier() != null) {
                psi.toStatementDescriptor(stmtIdentifier)?.let(descriptors::add)
              }
            }
          }
        }
        return@flatMap descriptors
      }
      .toTypedArray()
  }

  private fun PsiElement.toCreateTableDescriptor(createTableStmt: PsiElement?): FoldingDescriptor? {
    val openingBraceElement = createTableStmt?.node?.findChildByType(SqlTypes.LP) ?: return null
    val start = openingBraceElement.startOffset
    val end = nextSibling.endOffset
    if (start >= end) return null
    return FoldingDescriptor(this, start, end, null, "(...);")
  }

  private fun PsiElement.toCreateViewDescriptor(createViewStmt: PsiElement?): FoldingDescriptor? {
    val viewNameElement = (createViewStmt as? SqlCreateViewStmt)?.viewName ?: return null
    return toStatementDescriptor(viewNameElement)
  }

  private fun PsiElement.toCreateTriggerDescriptor(
    createTriggerStmt: PsiElement?
  ): FoldingDescriptor? {
    val triggerNameElement =
      (createTriggerStmt as? SqlCreateTriggerStmt)?.triggerName ?: return null
    return toStatementDescriptor(triggerNameElement)
  }

  private fun PsiElement.toCreateIndexDescriptor(createIndexStmt: PsiElement?): FoldingDescriptor? {
    val indexNameElement = (createIndexStmt as? SqlCreateIndexStmt)?.indexName ?: return null
    return toStatementDescriptor(indexNameElement)
  }

  private fun PsiElement.toStatementDescriptor(stmtIdentifier: PsiElement?): FoldingDescriptor? {
    if (stmtIdentifier == null) return null
    if (nextSibling?.node?.elementType != SqlTypes.SEMI) return null
    val start = stmtIdentifier.endOffset
    val end = nextSibling.endOffset
    if (start >= end) return null
    return FoldingDescriptor(this, start, end, null, "...")
  }

  private fun PsiElement.toImportListDescriptor(): FoldingDescriptor? {
    if (children.size < 2) return null
    val whitespaceElement = firstChild.getChildOfType<PsiWhiteSpace>() ?: return null
    val start = whitespaceElement.endOffset
    val end = lastChild.endOffset
    if (start >= end) return null
    return FoldingDescriptor(this, start, end, null, "...")
  }

  override fun getPlaceholderText(node: ASTNode) = "..."

  override fun isCollapsedByDefault(node: ASTNode) = with(node) {
    elementType == SqldelightTypes.IMPORT_STMT_LIST && getChildren(null).size > 1
  }
}
