package app.cash.sqldelight.gradle.squash

import app.cash.sqldelight.core.lang.util.findChildrenOfType
import app.cash.sqldelight.core.lang.util.range
import app.cash.sqldelight.dialect.api.MigrationSquasher
import app.cash.sqldelight.dialect.api.alteredTable
import com.alecstrong.sql.psi.core.SqlFileBase
import com.alecstrong.sql.psi.core.psi.SchemaContributor
import com.alecstrong.sql.psi.core.psi.SqlAlterTableRules
import com.alecstrong.sql.psi.core.psi.SqlNamedElementImpl
import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.intellij.psi.util.parentOfType
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset

class AnsiSqlMigrationSquasher(
  private val createNewSqlFile: (String) -> SqlFileBase,
) : MigrationSquasher {
  internal lateinit var squasher: MigrationSquasher

  override fun squish(
    statement: SqlStmt,
    currentFile: SqlFileBase,
  ): String {
    return when {
      statement.alterTableStmt != null -> {
        statement.alterTableStmt!!.alterTableRulesList.fold(currentFile) { currentFile, rules ->
          createNewSqlFile(squasher.squish(rules, into = currentFile))
        }.text
      }
      statement.dropIndexStmt != null -> {
        val create = currentFile.sqlStmtList!!.stmtList.mapNotNull { it.createIndexStmt }.single {
          it.indexName.textMatches(statement.dropIndexStmt!!.indexName!!.text)
        }
        currentFile.text.replaceRange(create.startOffset..create.endOffset, "")
      }
      statement.dropTableStmt != null -> {
        val create = currentFile.sqlStmtList!!.stmtList.mapNotNull { it.createTableStmt }.single {
          it.tableName.textMatches(statement.dropTableStmt!!.tableName!!.text)
        }
        val drops = currentFile.findChildrenOfType<SqlNamedElementImpl>()
          .filter { it.reference?.resolve() == create.tableName }
          .mapNotNull {
            it.parentOfType<SchemaContributor>()
          }
        drops.sortedByDescending { it.startOffset }.fold(currentFile.text) { fileText, element ->
          fileText.replaceRange(element.startOffset..element.endOffset, "")
        }
      }
      statement.dropTriggerStmt != null -> {
        val create = currentFile.sqlStmtList!!.stmtList.mapNotNull { it.createTriggerStmt }.single {
          it.triggerName.textMatches(statement.dropTriggerStmt!!.triggerName!!.text)
        }
        currentFile.text.replaceRange(create.startOffset..create.endOffset, "")
      }
      statement.dropViewStmt != null -> {
        val create = currentFile.sqlStmtList!!.stmtList.mapNotNull { it.createViewStmt }.single {
          it.viewName.textMatches(statement.dropViewStmt!!.viewName!!.text)
        }
        currentFile.text.replaceRange(create.startOffset..create.endOffset, "")
      }
      else -> {
        currentFile.text + statement.text + ";\n"
      }
    }
  }

  override fun squish(
    alterTableRules: SqlAlterTableRules,
    into: SqlFileBase,
  ): String {
    return when {
      alterTableRules.alterTableRenameTable != null -> {
        val tableName = alterTableRules.alteredTable(into).tableName
        val newName = alterTableRules.alterTableRenameTable!!.newTableName.text
        val elementsToRename = into.findChildrenOfType<SqlNamedElementImpl>().filter { it.reference?.resolve() == tableName }
        elementsToRename.sortedByDescending { it.startOffset }.fold(into.text) { fileText, element ->
          fileText.replaceRange(element.range, newName)
        }
      }
      alterTableRules.alterTableAddColumn != null -> {
        val createTable = alterTableRules.alteredTable(into)
        into.text.replaceRange(
          createTable.columnDefList.first().startOffset until createTable.columnDefList.last().endOffset,
          (createTable.columnDefList + alterTableRules.alterTableAddColumn!!.columnDef)
            .joinToString(separator = ",\n  ") { it.text },
        )
      }
      else -> throw IllegalStateException("Cannot squish ${alterTableRules.text}")
    }
  }
}
