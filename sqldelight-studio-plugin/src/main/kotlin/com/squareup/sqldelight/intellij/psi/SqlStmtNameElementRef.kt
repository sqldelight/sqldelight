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
import com.squareup.sqldelight.intellij.util.doRename
import com.squareup.sqldelight.intellij.util.findUsages

internal class SqlStmtNameElementRef(idNode: IdentifierElement, ruleName: String)
: SqliteElementRef(idNode, ruleName) {
  override val identifierDefinitionRule = RULE_ELEMENT_TYPES[SqliteParser.RULE_sql_stmt]

  override fun handleElementRename(newElementName: String): PsiElement {
    val file = myElement.containingFile as SqliteFile

    val usageInfo = myElement.findUsages(newElementName)
    myElement.doRename(newElementName, usageInfo, file, null)

    return myElement
  }
}
