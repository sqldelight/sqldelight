package app.cash.sqldelight.driver.native

internal interface Borrowed<T> {
  val value: T
  fun release()
}
