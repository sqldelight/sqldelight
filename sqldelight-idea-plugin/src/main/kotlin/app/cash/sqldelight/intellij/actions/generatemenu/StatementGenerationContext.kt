package app.cash.sqldelight.intellij.actions.generatemenu

import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt

data class StatementGenerationContext(
    val createTableStatements: List<SqlCreateTableStmt>,
    val activeStatement: SqlCreateTableStmt?,
) {
    val activeIndex = run {
        val activeIndex = createTableStatements.indexOfFirst { it == activeStatement }
        if (activeIndex == -1) {
            0
        } else {
            activeIndex
        }
    }
}
