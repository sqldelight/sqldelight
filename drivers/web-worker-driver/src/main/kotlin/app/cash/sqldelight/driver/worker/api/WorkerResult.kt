package app.cash.sqldelight.driver.worker.api

/**
 * The results of a SQL operation in the worker.
 */
internal external interface WorkerResult {
  /**
   * The "table" of values in the result, as rows of columns.
   * i.e. `values[row][col]`
   *
   * If the query returns no rows, then this should be an empty array.
   */
  var values: Array<Array<dynamic>>
}
