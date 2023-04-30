package app.cash.sqldelight.intellij.actions.generatemenu

import app.cash.sqldelight.core.capitalize
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt

/**
 * A class for building SQL statements. It takes a like of [SqlCreateTableStmt] as context for the file the statement
 * will be added to.
 */
class SQLStatementBuilder(private val createTableStatements: List<SqlCreateTableStmt>) {

  fun buildSelect(options: SelectQueryOptions): String {
    val queryTitle = createLabel("select", options.tableName)

    // sub-set selected, build full statement
    val columnNames = if (options.queryColumns.size < 4) {
      options.queryColumns.joinToString(", ")
    } else {
      options.queryColumns.joinToString(",\n       ")
    }

    val whereClause = if (options.whereColumns.isEmpty()) {
      ""
    } else {
      val separator = if (options.whereColumns.size < 4) {
        " AND "
      } else {
        "\n      AND "
      }
      options.whereColumns.joinToString(separator, prefix = "\nWHERE ") { "$it = :$it" }
    }

    return """$queryTitle:
        |SELECT $columnNames
        |FROM ${options.tableName}$whereClause;""".trimMargin()
  }

  fun buildInsert(options: InsertStatementOptions): String {
    val queryTitle = createLabel("insert", options.table.name())

    return if (options.selectedColumns.size == options.table.columnDefList.size) {
      // all columns selected, used shorthand
      """$queryTitle:
        |INSERT INTO ${options.table.name()}
        |VALUES ?;""".trimMargin()
    } else {
      // sub-set selected, build full statement
      val columnNames = if (options.selectedColumns.size > 3) {
        options.selectedColumns.joinToString(",\n    ", prefix = "\n    ", postfix = "\n")
      } else {
        options.selectedColumns.joinToString(", ")
      }
      val columnPlaceholders = options.selectedColumns.joinToString(", ") { "?" }

      """$queryTitle:
        |INSERT INTO ${options.table.name()} ($columnNames)
        |VALUES ($columnPlaceholders);""".trimMargin()
    }
  }

  fun buildUpdate(options: UpdateStatementOptions): String {
    val queryTitle = createLabel("update", options.tableName)

    // build statement
    val columnNames = options.updateColumns.joinToString(",\n    ") { "$it = :$it" }

    val whereClause = if (options.whereColumns.isEmpty()) {
      ""
    } else {
      val separator = if (options.whereColumns.size < 4) {
        " AND "
      } else {
        "\n      AND "
      }
      options.whereColumns.joinToString(separator, prefix = "\nWHERE ") { "$it = :$it" }
    }

    return """$queryTitle:
        |UPDATE ${options.tableName}
        |SET $columnNames$whereClause;""".trimMargin()
  }

  fun buildDelete(options: DeleteStatementOptions): String {
    val queryTitle = createLabel("delete", options.tableName)

    // build statement
    val whereClause = if (options.whereColumns.isEmpty()) {
      ""
    } else {
      val separator = if (options.whereColumns.size < 4) {
        " AND "
      } else {
        "\n      AND "
      }
      options.whereColumns.joinToString(separator, prefix = "\nWHERE ") { "$it = :$it" }
    }

    return """$queryTitle:
        |DELETE FROM ${options.tableName}$whereClause;""".trimMargin()
  }

  private fun createLabel(baseLabel: String, tableName: String): String {
    return if (createTableStatements.size == 1) {
      baseLabel
    } else {
      "$baseLabel${tableName.capitalize()}"
    }
  }

}

data class SelectQueryOptions(
  val tableName: String,
  val queryColumns: List<String>,
  val whereColumns: List<String>
)

data class UpdateStatementOptions(
  val tableName: String,
  val updateColumns: List<String>,
  val whereColumns: List<String>
)

data class InsertStatementOptions(val table: SqlCreateTableStmt, val selectedColumns: List<String>)

data class DeleteStatementOptions(val tableName: String, val whereColumns: List<String>)
