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
package com.squareup.sqldelight.intellij.psi

import com.intellij.psi.PsiElement
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.intellij.lang.SqliteFile
import com.squareup.sqldelight.intellij.lang.SqliteTokenTypes.RULE_ELEMENT_TYPES
import com.squareup.sqldelight.intellij.psi.SqliteElement.ColumnNameElement
import com.squareup.sqldelight.intellij.psi.SqliteElement.TableNameElement
import com.squareup.sqldelight.intellij.util.childOfType
import com.squareup.sqldelight.intellij.util.doRename
import com.squareup.sqldelight.intellij.util.elementType
import com.squareup.sqldelight.intellij.util.findUsages
import com.squareup.sqldelight.intellij.util.parentOfType
import com.squareup.sqldelight.intellij.util.prevSiblingOfType

internal class ColumnNameElementRef(idNode: IdentifierElement, ruleName: String)
: SqliteElementRef(idNode, ruleName) {
  private var leftTableDef: TableNameElement? = null

  override val identifierDefinitionRule = RULE_ELEMENT_TYPES[SqliteParser.RULE_column_def]

  override fun getVariants(): Array<Any> {
    setLeftTable()
    return super.getVariants()
  }

  override fun resolve(): PsiElement? {
    val columnName = element.parentOfType<ColumnNameElement>()
    if (columnName != null && columnName.parent.elementType === identifierDefinitionRule) {
      // If this is already a column definition return ourselves.
      return columnName
    }
    setLeftTable()

    return super.resolve()
  }

  override fun handleElementRename(newElementName: String): PsiElement {
    val file = myElement.containingFile as SqliteFile

    val usageInfo = myElement.findUsages(newElementName)
    myElement.doRename(newElementName, usageInfo, file, null)

    return myElement
  }

  override fun isAccepted(element: PsiElement) =
      when (leftTableDef) {
        null -> super.isAccepted(element) || element is TableNameElement
            && element.getParent().elementType === RULE_ELEMENT_TYPES[SqliteParser.RULE_create_table_stmt]
        else -> super.isAccepted(element)
            && leftTableDef!!.isSameTable(element.parent.parent.childOfType<TableNameElement>())
      }

  private fun setLeftTable() {
    leftTableDef = element.parentOfType<ColumnNameElement>()?.prevSiblingOfType<TableNameElement>()
  }
}
