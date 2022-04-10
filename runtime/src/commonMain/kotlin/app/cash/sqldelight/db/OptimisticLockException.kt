package app.cash.sqldelight.db

class OptimisticLockException(message: String?, cause: Throwable? = null) :
  IllegalStateException(message, cause)
