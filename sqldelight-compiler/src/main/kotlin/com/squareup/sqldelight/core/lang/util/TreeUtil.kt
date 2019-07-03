/*
 * Copyright (C) 2017 Square, Inc.
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
package com.squareup.sqldelight.core.lang.util

import com.alecstrong.sqlite.psi.core.psi.AliasElement
import com.alecstrong.sqlite.psi.core.psi.SqliteColumnName
import com.alecstrong.sqlite.psi.core.psi.SqliteCreateViewStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteCreateVirtualTableStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteTableName
import com.alecstrong.sqlite.psi.core.psi.SqliteTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.squareup.sqldelight.core.lang.IntermediateType
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.TEXT
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.psi.ColumnDefMixin
import com.squareup.sqldelight.core.lang.psi.InsertStmtMixin

internal inline fun <reified R: PsiElement> PsiElement.parentOfType(): R {
  return PsiTreeUtil.getParentOfType(this, R::class.java)!!
}

internal fun PsiElement.type(): IntermediateType = when (this) {
  is AliasElement -> source().type().copy(name = name)
  is SqliteColumnName -> {
    val parentRule = parent!!
    when (parentRule) {
      is ColumnDefMixin -> parentRule.type()
      is SqliteCreateVirtualTableStmt -> IntermediateType(TEXT, name = this.name)
      else -> reference!!.resolve()!!.type()
    }
  }
  is SqliteExpr -> type()
  else -> throw IllegalStateException("Cannot get function type for psi type ${this.javaClass}")
}

internal fun PsiElement.sqFile(): SqlDelightFile = containingFile as SqlDelightFile

inline fun <reified T: PsiElement> PsiElement.findChildrenOfType(): Collection<T> {
  return PsiTreeUtil.findChildrenOfType(this, T::class.java)
}

fun PsiElement.childOfType(type: IElementType): PsiElement? {
  return node.findChildByType(type)?.psi
}

fun PsiElement.childOfType(types: TokenSet): PsiElement? {
  return node.findChildByType(types)?.psi
}

inline fun <reified T: PsiElement> PsiElement.nextSiblingOfType(): T {
  return PsiTreeUtil.getNextSiblingOfType(this, T::class.java)!!
}

private fun PsiElement.rangesToReplace(): List<Pair<IntRange, String>> {
  return if (this is ColumnDefMixin && javaTypeName != null) {
    listOf(Pair(
        first = (typeName.node.startOffset + typeName.node.textLength) until
            (javaTypeName!!.node.startOffset + javaTypeName!!.node.textLength),
        second = ""
    ))
  } else if (this is InsertStmtMixin && acceptsTableInterface()) {
    listOf(Pair(
        first = childOfType(SqliteTypes.BIND_EXPR)!!.range,
        second = columns.joinToString(separator = ", ", prefix = "(", postfix = ")") { "?" }
    ))
  } else {
    children.flatMap { it.rangesToReplace() }
  }
}

private operator fun IntRange.minus(amount: Int): IntRange {
  return IntRange(start - amount, endInclusive - amount)
}

private val IntRange.length: Int
    get() = endInclusive - start + 1

val SQL_COMMENT_REGEX = "--.*\\n".toRegex()
val TRIM_WHITESPACE_REGEX = "\\s+".toRegex()

fun optimizeRawSqlText(rawSqlText: String): String {
    return rawSqlText
            .replace(SQL_COMMENT_REGEX, "")
            .replace(TRIM_WHITESPACE_REGEX, " ")
            .replace("( ", "(")
            .replace(" )", ")")
}

fun PsiElement.rawSqlText(
  replacements: List<Pair<IntRange, String>> = emptyList()
): String {
  val rawSqlText = (replacements + rangesToReplace())
      .sortedBy { it.first.start }
      .map { (range, replacement) -> (range - node.startOffset) to replacement }
      .fold(0 to text) { (totalRemoved, sqlText), (range, replacement) ->
        (totalRemoved + (range.length - replacement.length)) to sqlText.replaceRange(range - totalRemoved, replacement)
      }.second

  return optimizeRawSqlText(rawSqlText)
}

internal val PsiElement.range: IntRange
  get() = node.startOffset until (node.startOffset + node.textLength)

fun Collection<SqlDelightFile>.forInitializationStatements(
  body: (sqlText: String) -> Unit
) {
  val views = ArrayList<SqliteCreateViewStmt>()
  val creators = ArrayList<PsiElement>()

  forEach { file ->
    file.sqliteStatements().forEach statements@{ (label, sqliteStatement) ->
      if (label.name != null) return@statements

      when {
        sqliteStatement.createViewStmt != null -> views.add(sqliteStatement.createViewStmt!!)
        sqliteStatement.createTriggerStmt != null -> creators.add(sqliteStatement.createTriggerStmt!!)
        sqliteStatement.createIndexStmt != null -> creators.add(sqliteStatement.createIndexStmt!!)
        else -> body(sqliteStatement.rawSqlText())
      }
    }
  }

  val viewsLeft = views.map { it.viewName.name }.toMutableSet()
  while (views.isNotEmpty()) {
    views.removeAll { view ->
      if (view.compoundSelectStmt!!.findChildrenOfType<SqliteTableName>()
              .any { it.name in viewsLeft }) {
        return@removeAll false
      }
      body(view.rawSqlText())
      viewsLeft.remove(view.viewName.name)
      return@removeAll true
    }
  }

  creators.forEach { body(it.rawSqlText()) }
}
