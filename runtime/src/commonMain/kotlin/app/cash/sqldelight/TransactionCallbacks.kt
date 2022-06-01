package app.cash.sqldelight

interface TransactionCallbacks {
  fun afterCommit(function: () -> Unit)
  fun afterRollback(function: () -> Unit)
}
