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
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

private inline fun <reified T : PsiElement> PsiElement.parentOfType(withSelf: Boolean = false): T? {
  return PsiTreeUtil.getParentOfType(this, T::class.java, !withSelf)
}

class AnsiSqlMigrationSquasher(
  private val createNewSqlFile: (String) -> SqlFileBase,
) : MigrationSquasher {
  internal lateinit var squasher: MigrationSquasher

  override fun squish(
    statement: SqlStmt,
    into: SqlFileBase,
  ): String {
    return when {
      statement.alterTableStmt != null -> {
        statement.alterTableStmt!!.alterTableRulesList.fold(into) { currentFile, rules ->
          createNewSqlFile(squasher.squish(rules, into = currentFile))
        }.text
      }
      statement.dropIndexStmt != null -> {
        val create = into.sqlStmtList!!.stmtList.mapNotNull { it.createIndexStmt }.single {
          it.indexName.textMatches(statement.dropIndexStmt!!.indexName!!.text)
        }
        into.text.replaceRange(create.textRange.startOffset..create.textRange.endOffset, "")
      }
      statement.dropTableStmt != null -> {
        val create = into.sqlStmtList!!.stmtList.mapNotNull { it.createTableStmt }.single {
          it.tableName.textMatches(statement.dropTableStmt!!.tableName!!.text)
        }
        val drops = into.findChildrenOfType<SqlNamedElementImpl>()
          .filter { it.reference?.resolve() == create.tableName }
          .mapNotNull {
            it.parentOfType<SchemaContributor>()
          }
        drops.sortedByDescending { it.textRange.startOffset }.fold(into.text) { fileText, element ->
          fileText.replaceRange(element.textRange.startOffset..element.textRange.endOffset, "")
        }
      }
      statement.dropTriggerStmt != null -> {
        val create = into.sqlStmtList!!.stmtList.mapNotNull { it.createTriggerStmt }.single {
          it.triggerName.textMatches(statement.dropTriggerStmt!!.triggerName!!.text)
        }
        into.text.replaceRange(create.textRange.startOffset..create.textRange.endOffset, "")
      }
      statement.dropViewStmt != null -> {
        val create = into.sqlStmtList!!.stmtList.mapNotNull { it.createViewStmt }.single {
          it.viewName.textMatches(statement.dropViewStmt!!.viewName!!.text)
        }
        into.text.replaceRange(create.textRange.startOffset..create.textRange.endOffset, "")
      }
      else -> {
        into.text + statement.text + ";\n"
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
        elementsToRename.sortedByDescending { it.textRange.startOffset }.fold(into.text) { fileText, element ->
          fileText.replaceRange(element.range, newName)
        }
      }
      alterTableRules.alterTableAddColumn != null -> {
        val createTable = alterTableRules.alteredTable(into)
        into.text.replaceRange(
          createTable.columnDefList.first().textRange.startOffset until createTable.columnDefList.last().textRange.endOffset,
          (createTable.columnDefList + alterTableRules.alterTableAddColumn!!.columnDef)
            .joinToString(separator = ",\n  ") { it.text },
        )
      }
      else -> throw IllegalStateException("Cannot squish ${alterTableRules.text}")
    }
  }
}
