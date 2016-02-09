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
package com.squareup.sqlite.android.lang

import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.find.findUsages.JavaFindUsagesHandler
import com.intellij.find.findUsages.JavaFindUsagesHandlerFactory
import com.intellij.psi.PsiElement
import com.squareup.sqlite.android.psi.SqliteElement.ColumnNameElement
import com.squareup.sqlite.android.psi.SqliteElement.SqlStmtNameElement
import com.squareup.sqlite.android.util.getSecondaryElements

class SqliteFindUsagesHandlerFactory : FindUsagesHandlerFactory() {
  override fun canFindUsages(element: PsiElement) = element is ColumnNameElement
      || element is SqlStmtNameElement

  override fun createFindUsagesHandler(element: PsiElement, forHighlightUsages: Boolean) =
      when (element) {
        is ColumnNameElement -> JavaFindUsagesHandler(element, element.getSecondaryElements(),
            JavaFindUsagesHandlerFactory.getInstance(element.getProject()))
        is SqlStmtNameElement -> JavaFindUsagesHandler(element, element.getSecondaryElements(),
            JavaFindUsagesHandlerFactory.getInstance(element.getProject()))
        else -> null
      }
}
