package app.cash.sqldelight.driver.worker.api

@OptIn(ExperimentalJsExport::class)
@JsExport
/**
 * The results of a SQL operation in the worker.
 */
external interface WorkerResult {
  /**
   * The "table" of values in the result, as rows and columns.
   * i.e. `values[row][col]`
   *
   * The row/col layout of this array behaves the same as any other result set for a SQL operation.
   */
  var values: Array<Array<dynamic /* Number | String | Boolean | Uint8Array | Nothing? */>>
}
