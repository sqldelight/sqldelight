package app.cash.sqldelight.core.compiler

import app.cash.sqldelight.core.compiler.model.NamedMutator
import app.cash.sqldelight.core.lang.util.TableNameElement
import app.cash.sqldelight.core.lang.util.childOfType
import app.cash.sqldelight.core.lang.util.findChildrenOfType
import app.cash.sqldelight.core.lang.util.referencedTables
import com.alecstrong.sql.psi.core.psi.SqlForeignKeyClause
import com.alecstrong.sql.psi.core.psi.SqlTypes

class MutatorQueryGenerator(
  private val query: NamedMutator
) : ExecuteQueryGenerator(query) {
  override fun tablesUpdated(): List<TableNameElement> {
    val tablesUpdated = mutableListOf<TableNameElement>()
    val foreignKeyCascadeCheck = when (query) {
      is NamedMutator.Delete -> SqlTypes.DELETE
      is NamedMutator.Update -> SqlTypes.UPDATE
      else -> null
    }

    query.containingFile.iterateSqlFiles { psiFile ->
      val tablesAffected = query.tablesAffected.toMutableList()

      if (foreignKeyCascadeCheck != null) {
        psiFile.sqlStmtList?.stmtList?.mapNotNull { it.createTableStmt }?.forEach { table ->
          val effected = table.findChildrenOfType<SqlForeignKeyClause>().any {
            (it.foreignTable.name in query.tablesAffected.map { it.name }) && it.node.findChildByType(foreignKeyCascadeCheck) != null
          }
          if (effected) tablesAffected.add(TableNameElement.CreateTableName(table.tableName))
        }
      }

      psiFile.triggers.forEach { trigger ->
        if (trigger.tableName?.name in query.tablesAffected.map { it.name }) {
          val triggered = when (query) {
            is NamedMutator.Delete -> trigger.childOfType(SqlTypes.DELETE) != null
            is NamedMutator.Insert -> {
              trigger.childOfType(SqlTypes.INSERT) != null ||
                (query.hasUpsertClause && trigger.childOfType(SqlTypes.UPDATE) != null)
            }
            is NamedMutator.Update -> {
              val columns = trigger.columnNameList.map { it.name }
              val updateColumns = query.update.updateStmtSubsequentSetterList.map { it.columnName?.name } +
                query.update.columnNameList.map { it.name }
              trigger.childOfType(SqlTypes.UPDATE) != null && (
                columns.isEmpty() ||
                  updateColumns.any { it in columns }
                )
            }
          }

          if (triggered) {
            // Also need to notify for the trigger.
            tablesAffected += trigger.insertStmtList.map {
              it.tableName.referencedTables().single()
            }
            tablesAffected += trigger.deleteStmtList.map {
              it.qualifiedTableName!!.tableName.referencedTables().single()
            }
            tablesAffected += trigger.updateStmtList.map {
              it.qualifiedTableName.tableName.referencedTables().single()
            }
          }
        }
      }

      tablesUpdated.addAll(tablesAffected)
    }

    return tablesUpdated.distinctBy { it.name }
  }
}
