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
package com.squareup.sqldelight.lang

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
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.psi.SqliteElement
import com.squareup.sqldelight.util.childOfType
import com.squareup.sqldelight.util.childrenForRule

class SqlDelightStructureViewFactory : PsiStructureViewFactory {
  override fun getStructureViewBuilder(psiFile: PsiFile) = object : TreeBasedStructureViewBuilder() {
    override fun createStructureViewModel(editor: Editor?) = SqlDelightStructureViewModel(psiFile)
    override fun isRootNodeShown() = false
  }
}

internal class SqlDelightStructureViewModel(psiFile: PsiFile)
: StructureViewModelBase(psiFile,
    SqlDelightStructureViewElement(psiFile)), StructureViewModel.ElementInfoProvider {
  override fun isAlwaysLeaf(element: StructureViewTreeElement) = false
  override fun isAlwaysShowsPlus(element: StructureViewTreeElement) = false
}

internal class SqlDelightStructureViewElement(private val element: PsiElement) : StructureViewTreeElement, ItemPresentation, NavigationItem {
  override fun getValue() = element
  override fun getChildren() = when (element) {
    is SqliteFile -> element.childIdentifiers()
    else -> emptyArray<TreeElement>()
  }
  override fun canNavigateToSource() = (element as Navigatable).canNavigateToSource()
  override fun canNavigate() = (element as Navigatable).canNavigate()
  override fun navigate(requestFocus: Boolean) = (element as Navigatable).navigate(requestFocus)
  override fun getPresentation() = this
  override fun getName() = when (element) {
    is SqliteElement.TableNameElement -> "CREATE TABLE ${element.name}"
    is SqliteElement -> element.name
    is SqliteFile -> element.virtualFile.name
    else -> throw IllegalStateException("Unhandled element $element")
  }

  override fun getLocationString() = null
  override fun getIcon(open: Boolean) = element.getIcon(Iconable.ICON_FLAG_READ_STATUS)

  override fun getPresentableText() = name

  companion object {
    private fun SqliteFile.childIdentifiers() =
        (children[0].childrenForRule(SqliteParser.RULE_sql_stmt_list)[0]
            .childrenForRule(SqliteParser.RULE_sql_stmt).map {
          it.childOfType<SqliteElement.SqlStmtNameElement>()
        } + children[0].childrenForRule(SqliteParser.RULE_sql_stmt_list)[0]
            .childrenForRule(SqliteParser.RULE_create_table_stmt)[0]
            .childOfType<SqliteElement.TableNameElement>())
            .filterNotNull().map {
          SqlDelightStructureViewElement(it)
        }.toTypedArray()
  }
}
