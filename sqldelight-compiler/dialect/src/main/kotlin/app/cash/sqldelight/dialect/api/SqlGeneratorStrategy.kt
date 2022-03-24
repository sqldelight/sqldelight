package app.cash.sqldelight.dialect.api

interface SqlGeneratorStrategy {
  fun tableNameChanged(oldName: String, newName: String): String
  fun columnAdded(tableName: String, columnDef: String): String
  fun columnRemoved(tableName: String, columnName: String, columnDefList: List<String>): String
  fun columnNameChanged(
    tableName: String,
    oldName: String,
    newName: String,
    columnDefList: List<String>
  ): String
}
