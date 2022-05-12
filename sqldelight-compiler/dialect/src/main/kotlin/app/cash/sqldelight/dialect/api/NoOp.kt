package app.cash.sqldelight.dialect.api

internal class NoOp : SqlGeneratorStrategy {

  override fun tableNameChanged(oldName: String, newName: String): String {
    return ""
  }

  override fun columnAdded(tableName: String, columnDef: String): String {
    return ""
  }

  override fun columnRemoved(tableName: String, columnName: String, columnDefList: List<String>): String {
    return ""
  }

  override fun columnNameChanged(
    tableName: String,
    oldName: String,
    newName: String,
    columnDefList: List<String>
  ): String {
    return ""
  }
}
