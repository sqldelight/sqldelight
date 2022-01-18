package app.cash.sqldelight.intellij.run

internal class FakeStatementExecutor : SqlDelightStatementExecutor {

  var statement = ""
    private set

  override fun execute(sqlStmt: String) {
    statement = sqlStmt
  }
}