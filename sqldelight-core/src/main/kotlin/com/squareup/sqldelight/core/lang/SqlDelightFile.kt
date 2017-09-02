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
package com.squareup.sqldelight.core.lang

import com.alecstrong.sqlite.psi.core.psi.SqliteSqlStmt
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiDirectory
import com.intellij.psi.util.PsiTreeUtil

class SqlDelightFile(
    viewProvider: FileViewProvider
) : PsiFileBase(viewProvider, SqlDelightLanguage) {
  internal val packageName = parent!!.relativePathUnderSqlDelight().joinToString(".")

  override fun getFileType() = SqlDelightFileType

  internal fun sqliteStatements(): Collection<SqliteSqlStmt> {
    return PsiTreeUtil.findChildrenOfType(this, SqliteSqlStmt::class.java)
  }

  private fun PsiDirectory.relativePathUnderSqlDelight(): List<String> {
    if (name == "sqldelight") return emptyList()
    parent?.let { return it.relativePathUnderSqlDelight() + name }
    TODO("Give error that .sq file needs to be under sqldelight directory")
  }
}