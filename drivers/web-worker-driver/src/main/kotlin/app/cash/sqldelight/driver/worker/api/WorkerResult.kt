package app.cash.sqldelight.driver.worker.api

@OptIn(ExperimentalJsExport::class)
@JsExport
/**
 * The results of a SQL operation in the worker.
 */
external interface WorkerResult {
  /**
   * The "table" of values in the result, as rows of columns.
   * i.e. `values[row][col]`
   *
   * If the query returns no rows, then this should be an empty array.
   */
  var values: Array<Array<dynamic /* Number | String | Boolean | Uint8Array | Nothing? */>>
}
