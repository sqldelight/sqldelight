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
package com.squareup.sqldelight.intellij.lang

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.find.findUsages.JavaFindUsagesHandler
import com.intellij.find.findUsages.JavaFindUsagesHandlerFactory
import com.intellij.psi.PsiElement
import com.squareup.sqldelight.intellij.psi.IdentifierElement
import com.squareup.sqldelight.intellij.psi.SqliteElement.ColumnNameElement
import com.squareup.sqldelight.intellij.psi.SqliteElement.SqlStmtNameElement
import com.squareup.sqldelight.intellij.util.getSecondaryElements

/*
 * This takes precedence over SqliteFindUsagesProvider, and is being used separately
 * so we can take advantage of JavaFindUsagesHandler.
 */
class SqliteFindUsagesHandlerFactory : FindUsagesHandlerFactory() {
  override fun canFindUsages(element: PsiElement) = element is IdentifierElement &&
      (element.parent is ColumnNameElement || element.parent is SqlStmtNameElement)

  override fun createFindUsagesHandler(leaf: PsiElement, forHighlightUsages: Boolean): FindUsagesHandler? {
    val element = leaf.parent
    return when (element) {
      is ColumnNameElement -> JavaFindUsagesHandler(element, element.getSecondaryElements() + leaf,
          JavaFindUsagesHandlerFactory.getInstance(element.getProject()))
      is SqlStmtNameElement -> JavaFindUsagesHandler(element, element.getSecondaryElements() + leaf,
          JavaFindUsagesHandlerFactory.getInstance(element.getProject()))
      else -> null
    }
  }
}
