package com.squareup.sqldelight.core.compiler

import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.squareup.sqldelight.core.compiler.model.NamedMutator
import com.squareup.sqldelight.core.compiler.model.NamedQuery
import com.squareup.sqldelight.core.lang.util.childOfType
import com.squareup.sqldelight.core.lang.util.referencedTables

class MutatorQueryGenerator(
  private val query: NamedMutator
) : ExecuteQueryGenerator(query) {
  override fun queriesUpdated(): List<NamedQuery> {
    val resultSetsUpdated = mutableListOf<NamedQuery>()
    query.containingFile.iterateSqlFiles { psiFile ->
      val tablesAffected = mutableListOf(query.tableEffected)

      psiFile.triggers.forEach { trigger ->
        if (trigger.tableName?.name == query.tableEffected.name) {
          val triggered = when (query) {
            is NamedMutator.Delete -> trigger.childOfType(SqlTypes.DELETE) != null
            is NamedMutator.Insert -> trigger.childOfType(SqlTypes.INSERT) != null
            is NamedMutator.Update -> {
              val columns = trigger.columnNameList.map { it.name }
              val updateColumns = query.update.updateStmtSubsequentSetterList.map { it.columnName?.name } +
                query.update.columnName?.name
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

      resultSetsUpdated.addAll(
        psiFile.namedQueries
          .filter { query -> query.tablesObserved.any { it in tablesAffected } }
      )
    }

    return resultSetsUpdated
  }
}
