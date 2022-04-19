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
package app.cash.sqldelight.core.lang

import app.cash.sqldelight.core.SqlDelightFileIndex
import app.cash.sqldelight.core.compiler.model.BindableQuery
import app.cash.sqldelight.core.compiler.model.NamedExecute
import app.cash.sqldelight.core.compiler.model.NamedMutator.Delete
import app.cash.sqldelight.core.compiler.model.NamedMutator.Insert
import app.cash.sqldelight.core.compiler.model.NamedMutator.Update
import app.cash.sqldelight.core.compiler.model.NamedQuery
import app.cash.sqldelight.core.lang.psi.StmtIdentifierMixin
import app.cash.sqldelight.core.lang.util.argumentType
import app.cash.sqldelight.core.psi.SqlDelightStmtList
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlAnnotatedElement
import com.alecstrong.sql.psi.core.psi.SqlBindExpr
import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiDirectory
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil

class SqlDelightQueriesFile(
  viewProvider: FileViewProvider
) : SqlDelightFile(viewProvider, SqlDelightLanguage),
  SqlAnnotatedElement {
  override val packageName by lazy {
    module?.let { module ->
      SqlDelightFileIndex.getInstance(module).packageName(this)
    }
  }

  val namedQueries by lazy {
    transactions().filterIsInstance<NamedQuery>() + sqlStatements()
      .filter { typeResolver.queryWithResults(it.statement) != null && it.identifier.name != null }
      .map {
        NamedQuery(
          name = it.identifier.name!!,
          queryable = typeResolver.queryWithResults(it.statement)!!,
          statementIdentifier = it.identifier
        )
      }
  }

  internal val namedMutators by lazy {
    sqlStatements().filter { it.identifier.name != null && typeResolver.queryWithResults(it.statement) == null }
      .mapNotNull {
        when {
          it.statement.deleteStmtLimited != null -> Delete(it.statement.deleteStmtLimited!!, it.identifier)
          it.statement.insertStmt != null -> Insert(it.statement.insertStmt!!, it.identifier)
          it.statement.updateStmtLimited != null -> Update(it.statement.updateStmtLimited!!, it.identifier)
          else -> null
        }
      }
  }

  fun transactions(): Collection<BindableQuery> {
    val sqlStmtList = PsiTreeUtil.getChildOfType(this, SqlDelightStmtList::class.java)!!
    return sqlStmtList.stmtClojureList.map {
      val statements = it.stmtClojureStmtList!!.children.filterIsInstance<SqlStmt>()
      val lastQuery = typeResolver.queryWithResults(statements.last())
      if (lastQuery != null) {
        lastQuery.statement = it.stmtClojureStmtList!!
        return@map NamedQuery(
          statementIdentifier = it.stmtIdentifierClojure as StmtIdentifierMixin,
          queryable = lastQuery,
          name = it.stmtIdentifierClojure.name!!
        )
      } else {
        return@map NamedExecute(
          identifier = it.stmtIdentifierClojure as StmtIdentifierMixin,
          statement = it.stmtClojureStmtList!!
        )
      }
    }
  }

  internal val namedExecutes by lazy {
    val statements = sqlStatements()
      .filter {
        it.identifier.name != null &&
          it.statement.deleteStmtLimited == null &&
          it.statement.insertStmt == null &&
          it.statement.updateStmtLimited == null &&
          it.statement.compoundSelectStmt == null
      }
      .map { NamedExecute(it.identifier, it.statement) }

    return@lazy transactions().filterIsInstance<NamedExecute>() + statements
  }

  /**
   * A collection of all the adapters needed for arguments or result columns in this query.
   */
  internal val requiredAdapters by lazy {
    val argumentAdapters = PsiTreeUtil.findChildrenOfType(this, SqlBindExpr::class.java)
      .mapNotNull { typeResolver.argumentType(it).parentAdapter() }

    val resultColumnAdapters = namedQueries.flatMap { it.resultColumnRequiredAdapters }

    return@lazy (argumentAdapters + resultColumnAdapters).distinct()
  }

  internal val triggers by lazy { triggers(this) }

  override val order = null

  override fun getFileType() = SqlDelightFileType

  internal fun sqlStatements(): Collection<LabeledStatement> {
    val sqlStmtList = PsiTreeUtil.getChildOfType(this, SqlDelightStmtList::class.java)!!
    return sqlStmtList.stmtIdentifierList.zip(sqlStmtList.stmtList) { id, stmt ->
      return@zip LabeledStatement(id as StmtIdentifierMixin, stmt)
    }
  }

  fun iterateSqlFiles(block: (SqlDelightQueriesFile) -> Unit) {
    val module = module ?: return

    fun PsiDirectory.iterateSqlFiles() {
      children.forEach {
        if (it is PsiDirectory) it.iterateSqlFiles()
        if (it is SqlDelightQueriesFile) block(it)
      }
    }

    SqlDelightFileIndex.getInstance(module).sourceFolders(this).forEach { dir ->
      dir.iterateSqlFiles()
    }
  }

  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    if (packageName.isNullOrEmpty()) {
      annotationHolder.createErrorAnnotation(this, "SqlDelight files must be placed in a package directory.")
    }
  }

  override fun searchScope(): GlobalSearchScope {
    val module = module
    if (module != null && !SqlDelightFileIndex.getInstance(module).deriveSchemaFromMigrations) {
      return GlobalSearchScope.getScopeRestrictedByFileTypes(super.searchScope(), SqlDelightFileType)
    }
    return super.searchScope()
  }

  data class LabeledStatement(val identifier: StmtIdentifierMixin, val statement: SqlStmt)
}
