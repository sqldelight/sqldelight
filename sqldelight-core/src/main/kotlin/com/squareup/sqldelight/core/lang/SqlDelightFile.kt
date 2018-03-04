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

import com.alecstrong.sqlite.psi.core.SqliteFileBase
import com.alecstrong.sqlite.psi.core.psi.SqliteSqlStmt
import com.intellij.openapi.module.Module
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.LightVirtualFile
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.SqlDelightProjectService
import com.squareup.sqldelight.core.lang.psi.StmtIdentifierMixin
import com.squareup.sqldelight.core.psi.SqlDelightSqlStmtList

class SqlDelightFile(
    viewProvider: FileViewProvider
) : SqliteFileBase(viewProvider, SqlDelightLanguage) {
  private val module: Module
    get() = SqlDelightProjectService.getInstance(project).module(virtualFile)

  internal val packageName by lazy { SqlDelightFileIndex.getInstance(module).packageName(this) }
  internal val generatedDir by lazy { packageName.replace('.', '/') }

  override fun getFileType() = SqlDelightFileType

  internal fun sqliteStatements(): Collection<LabeledStatement> {
    if (virtualFile == null || virtualFile is LightVirtualFile) return emptyList()

    val sqlStmtList = PsiTreeUtil.getChildOfType(this, SqlDelightSqlStmtList::class.java)!!
    return sqlStmtList.stmtIdentifierList.zip(sqlStmtList.sqlStmtList) { id, stmt ->
      return@zip LabeledStatement(id as StmtIdentifierMixin, stmt)
    }
  }

  public override fun iterateSqliteFiles(iterator: (PsiFile) -> Boolean) {
    if (virtualFile == null || virtualFile is LightVirtualFile) return

    SqlDelightFileIndex.getInstance(module).sourceFolders().forEach { sqldelightDirectory ->
      if (!PsiTreeUtil.findChildrenOfType(sqldelightDirectory, SqlDelightFile::class.java)
          .all { iterator(it) }) {
        return@forEach
      }
    }
  }

  data class LabeledStatement(val identifier: StmtIdentifierMixin, val statement: SqliteSqlStmt)
}