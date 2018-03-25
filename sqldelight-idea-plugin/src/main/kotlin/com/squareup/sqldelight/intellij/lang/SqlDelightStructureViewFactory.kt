/*
 * Copyright (C) 2018 Square, Inc.
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
package com.squareup.sqldelight.intellij.lang

import com.alecstrong.sqlite.psi.core.psi.SqliteCreateTableStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteCreateVirtualTableStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteIdentifier
import com.alecstrong.sqlite.psi.core.psi.SqliteIndexName
import com.alecstrong.sqlite.psi.core.psi.SqliteStatement
import com.alecstrong.sqlite.psi.core.psi.SqliteTableName
import com.alecstrong.sqlite.psi.core.psi.SqliteTriggerName
import com.alecstrong.sqlite.psi.core.psi.SqliteViewName
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Iconable
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.psi.SqlDelightSqlStmtList
import com.squareup.sqldelight.core.psi.SqlDelightStmtIdentifier
import com.squareup.sqldelight.intellij.util.childOfType
import javax.swing.Icon

class SqlDelightStructureViewFactory : PsiStructureViewFactory {
  override fun getStructureViewBuilder(psiFile: PsiFile) = object : TreeBasedStructureViewBuilder() {
    override fun createStructureViewModel(editor: Editor?) = SqlDelightStructureViewModel(psiFile)
    override fun isRootNodeShown() = false
  }
}

internal class SqlDelightStructureViewModel(
  psiFile: PsiFile
) : StructureViewModelBase(psiFile, SqlDelightStructureViewElement(psiFile)),
    StructureViewModel.ElementInfoProvider {
  override fun isAlwaysLeaf(element: StructureViewTreeElement) = false
  override fun isAlwaysShowsPlus(element: StructureViewTreeElement) = false
}

internal class SqlDelightStructureViewElement(
  private val element: PsiElement
) : StructureViewTreeElement,
    ItemPresentation,
    NavigationItem {
  override fun getValue() = element
  override fun getChildren() = when (element) {
    is SqlDelightFile -> element.childIdentifiers()
    else -> emptyArray()
  }

  override fun canNavigateToSource() = (element as Navigatable).canNavigateToSource()
  override fun canNavigate() = (element as Navigatable).canNavigate()
  override fun navigate(requestFocus: Boolean) = (element as Navigatable).navigate(requestFocus)
  override fun getPresentation() = this
  override fun getName(): String? = when (element) {
    is SqlDelightFile -> element.virtualFile?.name
    is SqliteTableName -> when (element.parent) {
      is SqliteCreateTableStmt -> "CREATE TABLE ${element.name}"
      is SqliteCreateVirtualTableStmt -> "CREATE VIRTUAL TABLE ${element.name}"
      else -> throw IllegalStateException(
          "Unhandled table name element for parent ${element.parent}")
    }
    is SqliteIndexName -> "CREATE INDEX ${element.text}"
    is SqliteTriggerName -> "CREATE TRIGGER ${element.text}"
    is SqliteViewName -> "CREATE VIEW ${element.name}"
    is SqliteIdentifier -> element.text
    else -> throw IllegalStateException("Unhandled element $element")
  }

  override fun getLocationString(): String? = null
  override fun getIcon(open: Boolean): Icon? = element.getIcon(Iconable.ICON_FLAG_READ_STATUS)

  override fun getPresentableText() = name

  companion object {
    private fun SqlDelightFile.childIdentifiers(): Array<TreeElement> {
      val statementList = childOfType<SqlDelightSqlStmtList>() ?: return emptyArray()
      return statementList.children
          .mapNotNull {
            val element = when (it) {
              is SqlDelightStmtIdentifier -> it.childOfType<SqliteIdentifier>()
              is SqliteStatement -> with(it.sqlStmt) {
                when {
                  createTableStmt != null -> createTableStmt?.tableName
                  createIndexStmt != null -> createIndexStmt?.indexName
                  createTriggerStmt != null -> createTriggerStmt?.triggerName
                  createViewStmt != null -> createViewStmt?.viewName
                  createVirtualTableStmt != null -> createVirtualTableStmt?.tableName
                  else -> null
                }
              }
              else -> null
            }
            return@mapNotNull element?.let(::SqlDelightStructureViewElement)
          }
          .toTypedArray()
    }
  }
}
