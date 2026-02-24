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
package app.cash.sqldelight.core.lang.util

import app.cash.sqldelight.core.lang.MigrationFile
import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.core.lang.SqlDelightQueriesFile
import app.cash.sqldelight.core.lang.acceptsTableInterface
import app.cash.sqldelight.core.lang.psi.ColumnTypeMixin
import app.cash.sqldelight.core.lang.psi.InsertStmtValuesMixin
import app.cash.sqldelight.dialect.api.ExposableType
import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PreCreateTableInitialization
import app.cash.sqldelight.dialect.api.PrimitiveType
import app.cash.sqldelight.dialect.api.PrimitiveType.INTEGER
import app.cash.sqldelight.dialect.api.PrimitiveType.REAL
import app.cash.sqldelight.dialect.api.PrimitiveType.TEXT
import app.cash.sqldelight.dialect.api.TableFunctionRowType
import app.cash.sqldelight.dialect.grammar.mixins.BindParameterMixin
import com.alecstrong.sql.psi.core.psi.AliasElement
import com.alecstrong.sql.psi.core.psi.SqlAnnotatedElement
import com.alecstrong.sql.psi.core.psi.SqlBindExpr
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.alecstrong.sql.psi.core.psi.SqlCreateViewStmt
import com.alecstrong.sql.psi.core.psi.SqlCreateVirtualTableStmt
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.alecstrong.sql.psi.core.psi.SqlExtensionStmt
import com.alecstrong.sql.psi.core.psi.SqlModuleArgument
import com.alecstrong.sql.psi.core.psi.SqlModuleColumnDef
import com.alecstrong.sql.psi.core.psi.SqlPragmaName
import com.alecstrong.sql.psi.core.psi.SqlResultColumn
import com.alecstrong.sql.psi.core.psi.SqlTableName
import com.alecstrong.sql.psi.core.psi.SqlTypeName
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.alecstrong.sql.psi.core.psi.mixins.ColumnDefMixin
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.DirectedAcyclicGraph
import org.jgrapht.traverse.TopologicalOrderIterator

internal fun PsiElement.type(): IntermediateType = when (this) {
  is ExposableType -> type()
  is SqlTypeName -> sqFile().typeResolver.definitionType(this)
  is AliasElement -> source().type().copy(name = name)
  is ColumnDefMixin -> (columnType as ColumnTypeMixin).type()
  is SqlPragmaName -> IntermediateType(TEXT)
  is SqlColumnName -> {
    when (val parentRule = parent) {
      is ColumnDefMixin -> parentRule.type()
      is SqlModuleColumnDef -> IntermediateType(TEXT, name = this.name).asNullable()
      else -> {
        when (val resolvedReference = reference?.resolve()) {
          null -> IntermediateType(PrimitiveType.NULL)
          // Synthesized columns refer directly to the table
          is SqlCreateTableStmt,
          is SqlCreateVirtualTableStmt,
          -> synthesizedColumnType(this.name)
          else -> {
            val columnSelected = queryAvailable(this).flatMap { it.columns }
              .firstOrNull { it.element == resolvedReference }
            columnSelected?.nullable?.let {
              resolvedReference.type().nullableIf(it)
            } ?: resolvedReference.type()
          }
        }
      }
    }
  }
  is TableFunctionRowType -> (sqFile().typeResolver.definitionType(columnType()).asNullable())
  is SqlExpr -> sqFile().typeResolver.resolvedType(this)
  is SqlResultColumn -> sqFile().typeResolver.resolvedType(expr!!)
  else -> throw IllegalStateException("Cannot get function type for psi type ${this.javaClass}")
}

private fun synthesizedColumnType(columnName: String): IntermediateType {
  val dialectType = when (columnName) {
    "docid", "rowid", "oid", "_rowid_" -> INTEGER
    "rank" -> REAL
    else -> TEXT
  }

  return IntermediateType(dialectType, name = columnName)
}

fun PsiDirectory.queryFiles(): Sequence<SqlDelightQueriesFile> {
  return children.asSequence().flatMap {
    when (it) {
      is PsiDirectory -> it.queryFiles()
      is SqlDelightQueriesFile -> listOf(it).asSequence()
      else -> emptySequence()
    }
  }
}

fun PsiDirectory.migrationFiles(): Sequence<MigrationFile> {
  return children.asSequence().flatMap {
    when (it) {
      is PsiDirectory -> it.migrationFiles()
      is MigrationFile -> listOf(it).asSequence()
      else -> emptySequence()
    }
  }
}

internal fun PsiElement.sqFile(): SqlDelightFile = containingFile as SqlDelightFile

inline fun <reified T : SqlAnnotatedElement> PsiElement.findChildrenOfType(): Collection<T> {
  return PsiTreeUtil.findChildrenOfType(this, T::class.java)
}

inline fun <reified T : SqlAnnotatedElement> PsiElement.findChildOfType(): T? {
  return PsiTreeUtil.findChildOfType(this, T::class.java)
}

fun PsiElement.childOfType(type: IElementType): PsiElement? {
  return node.findChildByType(type)?.psi
}

fun PsiElement.childOfType(types: TokenSet): PsiElement? {
  return node.findChildByType(types)?.psi
}

fun ASTNode.findChildRecursive(type: IElementType): ASTNode? {
  getChildren(null).forEach {
    if (it.elementType == type) return it
    it.findChildByType(type)?.let { return it }
  }
  return null
}

inline fun <reified T : PsiElement> PsiElement.nextSiblingOfType(): T {
  return PsiTreeUtil.getNextSiblingOfType(this, T::class.java)!!
}

private fun PsiElement.rangesToReplace(): List<Pair<IntRange, String>> {
  val starExpansionEnabled = this.sqFile().expandSelectStar
  return if (starExpansionEnabled && this is SqlCreateViewStmt) {
    emptyList()
  } else if (this is ColumnTypeMixin && javaTypeName != null) {
    listOf(
      Pair(
        first = (typeName.node.startOffset + typeName.node.textLength) until
          (javaTypeName!!.node.startOffset + javaTypeName!!.node.textLength),
        second = "",
      ),
    )
  } else if (this is ColumnTypeMixin && node.getChildren(null).any { it.text == "VALUE" || it.text == "LOCK" }) {
    listOf(
      Pair(
        first = (typeName.node.startOffset + typeName.node.textLength) until
          (node.getChildren(null).single { it.text == "VALUE" || it.text == "LOCK" }.let { it.startOffset + it.textLength }),
        second = "",
      ),
    )
  } else if (this is SqlModuleArgument && moduleArgumentDef?.columnDef != null && (parent as SqlCreateVirtualTableStmt).moduleName?.text?.lowercase() == "fts5") {
    val columnDef = moduleArgumentDef!!.columnDef!!
    // If there is a space at the end of the column constraint "TEXT NOT NULL ", preserve it as this means it could have a "TEXT NOT NULL UNINDEXED" constraint
    val lengthModifier = if (columnDef.columnConstraintList.isNotEmpty() && columnDef.columnConstraintList.last().text.endsWith(" ")) 1 else 0
    listOf(
      Pair(
        first = (columnDef.columnName.node.startOffset + columnDef.columnName.node.textLength) until
          (columnDef.columnName.node.startOffset + columnDef.node.textLength - lengthModifier),
        second = "",
      ),
    )
  } else if (this is InsertStmtValuesMixin && parent?.acceptsTableInterface() == true) {
    val generateAsync = this.sqFile().generateAsync
    val bindExpr = childOfType(SqlTypes.BIND_EXPR) as SqlBindExpr
    val bindParameterMixin = bindExpr.bindParameter as BindParameterMixin
    buildList {
      if (parent!!.columnNameList.isEmpty()) {
        add(
          Pair(
            first = parent!!.tableName.range,
            second = parent!!.columns.joinToString(
              separator = ", ",
              prefix = "${parent!!.tableName.node.text} (",
              postfix = ")",
            ) { it.node.text },
          ),
        )
      }
      add(
        Pair(
          first = bindExpr.range,
          second = (1..parent!!.columns.size).joinToString(separator = ", ", prefix = "(", postfix = ")") { bindParameterMixin.replaceWith(generateAsync, it) },
        ),
      )
    }
  } else if (starExpansionEnabled && this is SqlResultColumn && this.expr == null) {
    selectStarExpansions(this)
  } else {
    children.flatMap { it.rangesToReplace() }
  }
}

private fun PsiElement.selectStarExpansions(resultColumns: SqlResultColumn): List<Pair<IntRange, String>> {
  return listOf(
    this.range to resultColumns.queryExposed().flatMap { query ->
      query.columns.map { column ->
        val columnElement = column.element as? PsiNamedElement ?: return@selectStarExpansions emptyList()

        buildString {
          if (query.table != null) {
            append("${query.table!!.node.text}.")
          } else {
            val definition = columnElement.reference?.resolve()
            if (definition?.parent is SqlCreateViewStmt) {
              append("${(definition.parent as SqlCreateViewStmt).viewName.node.text}.")
            } else if (definition?.parent?.parent is SqlCreateTableStmt) {
              append("${(definition.parent.parent as SqlCreateTableStmt).tableName.node.text}.")
            }
          }
          append(columnElement.node.text)
        }
      }
    }.joinToString(separator = ", "),
  )
}

private operator fun IntRange.minus(amount: Int): IntRange {
  return IntRange(start - amount, endInclusive - amount)
}

private val IntRange.length: Int
  get() = endInclusive - start + 1

fun PsiElement.rawSqlText(
  replacements: List<Pair<IntRange, String>> = emptyList(),
): String {
  val x = (replacements + rangesToReplace())
    .sortedBy { it.first.first }
    .map { (range, replacement) -> (range - node.startOffset) to replacement }

  val y = x.fold(
    0 to text,
    { (totalRemoved, sqlText), (range, replacement) ->
      (totalRemoved + (range.length - replacement.length)) to sqlText.replaceRange(range - totalRemoved, replacement)
    },
  ).second
  return y
}

val PsiElement.range: IntRange
  get() = node.startOffset until (node.startOffset + node.textLength)

fun SqlExtensionStmt.hasPreCreateTableInitialization(): Boolean {
  return PsiTreeUtil.getChildOfType(
    this,
    PreCreateTableInitialization::class.java,
  ) != null
}

fun Collection<SqlDelightQueriesFile>.forInitializationStatements(
  allowReferenceCycles: Boolean,
  body: (sqlText: String) -> Unit,
) {
  val views = ArrayList<SqlCreateViewStmt>()
  val preTables = ArrayList<PsiElement>()
  val tables = ArrayList<SqlCreateTableStmt>()
  val creators = ArrayList<PsiElement>()
  val miscellaneous = ArrayList<PsiElement>()

  forEach { file ->
    file.sqlStatements()
      .filter { (label, _) -> label.name == null }
      .forEach { (_, sqlStatement) ->
        when {
          sqlStatement.extensionStmt != null &&
            sqlStatement.extensionStmt!!.hasPreCreateTableInitialization() -> preTables.add(sqlStatement.extensionStmt!!)
          sqlStatement.createTableStmt != null -> tables.add(sqlStatement.createTableStmt!!)
          sqlStatement.createViewStmt != null -> views.add(sqlStatement.createViewStmt!!)
          sqlStatement.createTriggerStmt != null -> creators.add(sqlStatement.createTriggerStmt!!)
          sqlStatement.createIndexStmt != null -> creators.add(sqlStatement.createIndexStmt!!)
          else -> miscellaneous.add(sqlStatement)
        }
      }
  }

  preTables.forEach { body(it.rawSqlText()) }

  when (allowReferenceCycles) {
    // If we allow cycles, don't attempt to order the table creation statements. The dialect
    // is permissive.
    true -> tables.forEach { body(it.rawSqlText()) }
    false -> tables.buildGraph().topological().forEach { body(it.rawSqlText()) }
  }

  views.orderStatements(
    { it.viewName.name },
    { view: SqlCreateViewStmt -> view.compoundSelectStmt!!.findChildrenOfType<SqlTableName>() },
    { it.name },
    body,
  )

  creators.forEach { body(it.rawSqlText()) }
  miscellaneous.forEach { body(it.rawSqlText()) }
}

private fun ArrayList<SqlCreateTableStmt>.buildGraph(): Graph<SqlCreateTableStmt, DefaultEdge> {
  val graph = DirectedAcyclicGraph<SqlCreateTableStmt, DefaultEdge>(DefaultEdge::class.java)
  val namedStatements = this.associateBy { it.tableName.name }

  this.forEach { table ->
    graph.addVertex(table)
    table.columnDefList.forEach { column ->
      (column.columnConstraintList.mapNotNull { it.foreignKeyClause?.foreignTable } + table.tableConstraintList.mapNotNull { it.foreignKeyClause?.foreignTable }).forEach { fk ->
        try {
          val foreignTable = namedStatements[fk.name]
          graph.apply {
            addVertex(foreignTable)
            addEdge(foreignTable, table)
          }
        } catch (e: IllegalArgumentException) {
          error("Detected cycle between ${table.tableName.name}.${column.columnName.name} and ${fk.name}. Consider lifting the foreign key constraint out of the table definition.")
        }
      }
    }
  }

  return graph
}

private fun <V, E> Graph<V, E>.topological(): Iterator<V> = TopologicalOrderIterator(this)

private fun <T : PsiElement, E : PsiElement> ArrayList<T>.orderStatements(
  nameSelector: (T) -> String,
  relationIdentifier: (T) -> Collection<E>,
  relatedNameSelector: (E) -> String,
  body: (sqlText: String) -> Unit,
) {
  val statementsLeft = this.map(nameSelector).toMutableSet()
  while (this.isNotEmpty()) {
    this.removeAll { statement ->
      val relatedStatements = relationIdentifier(statement)
      if (relatedStatements.any { relatedNameSelector(it) in statementsLeft }) {
        return@removeAll false
      }

      body(statement.rawSqlText())
      statementsLeft.remove(nameSelector(statement))
      return@removeAll true
    }
  }
}
