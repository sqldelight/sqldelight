package app.cash.sqldelight.intellij.refactoring.strategy

class Sqlite : SqlGeneratorStrategy {

  override fun tableNameChanged(oldName: String, newName: String): String {
    return "ALTER TABLE $oldName RENAME TO $newName;"
  }

  override fun columnAdded(tableName: String, columnDef: String): String {
    return "ALTER TABLE $tableName ADD COLUMN $columnDef;"
  }

  override fun columnRemoved(
    tableName: String,
    columnName: String,
    columnDefList: List<String>
  ): String {
    val columnNames = columnDefList.filter { !it.startsWith(columnName) }
      .joinToString(", ") { columnDef -> columnDef.takeWhile { !Character.isWhitespace(it) } }
    val columnDefString = columnDefList.filter { !it.startsWith(columnName) }
      .joinToString(", ")

    return """
      |BEGIN TRANSACTION;
      |CREATE TABLE tmp_$tableName ($columnDefString);
      |INSERT INTO tmp_$tableName ($columnNames) SELECT ($columnNames) FROM $tableName;
      |DROP TABLE $tableName;
      |ALTER TABLE tmp_$tableName RENAME TO $tableName;
      |COMMIT TRANSACTION;
    """.trimMargin()
  }

  override fun columnNameChanged(
    tableName: String,
    oldName: String,
    newName: String,
    columnDefList: List<String>
  ): String {
    val newColumnDefList = columnDefList.toMutableList()
    val index = newColumnDefList.indexOfFirst { it.startsWith(oldName) }
    newColumnDefList[index] = newColumnDefList[index].replace(oldName, newName)

    val newColumnDefString = newColumnDefList.joinToString(", ")
    val newColumnNames = newColumnDefList.joinToString(", ") { columnDef ->
      columnDef.takeWhile { !Character.isWhitespace(it) }
    }
    val oldColumnNames = columnDefList.joinToString(", ") { columnDef ->
      columnDef.takeWhile { !Character.isWhitespace(it) }
    }
    return """
      |BEGIN TRANSACTION;
      |CREATE TABLE tmp_$tableName ($newColumnDefString);
      |INSERT INTO tmp_$tableName ($newColumnNames) SELECT ($oldColumnNames) FROM $tableName;
      |DROP TABLE $tableName;
      |ALTER TABLE tmp_$tableName RENAME TO $tableName;
      |COMMIT TRANSACTION;
    """.trimMargin()
  }
}
