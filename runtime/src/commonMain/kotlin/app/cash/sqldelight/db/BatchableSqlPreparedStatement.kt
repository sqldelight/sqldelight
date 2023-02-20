package app.cash.sqldelight.db

/**
 * A [SqlPreparedStatement] that supports batching.
 */
interface BatchableSqlPreparedStatement : SqlPreparedStatement {
  /**
   * Adds the current set of bound parameters to the batch of commands that will be executed with this statement.
   */
  fun addBatch()
}
