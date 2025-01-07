package app.cash.sqldelight.driver.androidx.sqlite

import app.cash.sqldelight.Transacter

internal actual class TransactionsThreadLocal actual constructor() {
  private val transactions = ThreadLocal<Transacter.Transaction>()

  internal actual fun get(): Transacter.Transaction? = transactions.get()

  internal actual fun set(transaction: Transacter.Transaction?) {
    transactions.set(transaction)
  }
}
