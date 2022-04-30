package app.cash.sqldelight.driver.r2dbc

import app.cash.sqldelight.AsyncTransacter
import app.cash.sqldelight.Query
import app.cash.sqldelight.db.AsyncSqlDriver
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlPreparedStatement
import io.r2dbc.spi.Connection
import io.r2dbc.spi.Statement
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

class R2dbcDriver(val connection: Connection) : AsyncSqlDriver {
  override fun <R> executeQuery(
    identifier: Int?,
    sql: String,
    mapper: (SqlCursor) -> R,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?
  ): AsyncSqlDriver.Callback<R> {
    val prepared = connection.createStatement(sql).also { statement ->
      R2dbcPreparedStatement(statement).apply { if (binders != null) this.binders() }
    }
    return AsyncSqlDriver.SimpleCallback { callback ->
      prepared.execute().subscribe(next = { result ->
        val rowSet = mutableListOf<Map<Int, Any?>>()
        result.map { row, rowMetadata ->
          rowSet.add(rowMetadata.columnMetadatas.mapIndexed { index, _ -> index to row.get(index) }.toMap())
        }.subscribe(complete = {
          try {
            callback.success(mapper(R2dbcCursor(rowSet)))
          } catch (e: Exception) {
            callback.error(e)
          }
        }, error = { callback.error(it) })
      }, error = { callback.error(it) })
    }
  }

  override fun execute(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?
  ): AsyncSqlDriver.Callback<Long> {
    val prepared = connection.createStatement(sql).also { statement ->
      R2dbcPreparedStatement(statement).apply { if (binders != null) this.binders() }
    }

    return AsyncSqlDriver.SimpleCallback<Long> { callback ->
      prepared.execute().subscribe(next = { result ->
        /*val complete = AtomicBoolean(false)
        result.rowsUpdated.subscribe(
                next = {
                  callback.success(it)
                  complete.set(true)
                },
                error = { callback.error(it) },
                complete = { if (!complete.get()) callback.success(0) })*/
        // TODO: r2dbc-mysql emits a java.lang.Integer instead of a java.lang.Long, mysql driver needs to support latest r2dbc-spi
        callback.success(0)
      }, error = { callback.error(it) })
    }
  }

  private val transactions = ThreadLocal<Transaction>()
  var transaction: Transaction?
    get() = transactions.get()
    set(value) {
      transactions.set(value)
    }

  override fun newTransaction(): AsyncSqlDriver.Callback<out AsyncTransacter.Transaction> {
    val enclosing = transaction
    val transaction = Transaction(enclosing, this.connection)

    return AsyncSqlDriver.SimpleCallback<Transaction> { callback ->
      if (enclosing == null) {
        connection.beginTransaction().subscribe(next = {
          this@R2dbcDriver.transaction = transaction
          callback.success(transaction)
        }, error = callback::error)
      }
    }
  }

  override fun currentTransaction(): AsyncTransacter.Transaction? = transaction

  override fun addListener(listener: Query.Listener, queryKeys: Array<String>) {
  }

  override fun removeListener(listener: Query.Listener, queryKeys: Array<String>) {
  }

  override fun notifyListeners(queryKeys: Array<String>) {
  }

  override fun close() {
  }

  class Transaction(
    override val enclosingTransaction: AsyncTransacter.Transaction?,
    val connection: Connection
  ) : AsyncTransacter.Transaction() {
    override fun endTransaction(successful: Boolean): AsyncSqlDriver.Callback<Unit> =
      AsyncSqlDriver.SimpleCallback { callback ->
        if (enclosingTransaction == null) {
          if (successful) connection.commitTransaction().subscribe(next = {
            callback.success(Unit)
          }, error = callback::error)
          else connection.rollbackTransaction().subscribe(next = {
            callback.success(Unit)
          }, error = callback::error)
        }
      }
  }
}

open class R2dbcPreparedStatement(private val statement: Statement) : SqlPreparedStatement {
  override fun bindBytes(index: Int, bytes: ByteArray?) {
    if (bytes == null) {
      statement.bindNull(index - 1, ByteArray::class.java)
    } else {
      statement.bind(index - 1, bytes)
    }
  }

  override fun bindLong(index: Int, long: Long?) {
    if (long == null) {
      statement.bindNull(index - 1, Long::class.java)
    } else {
      statement.bind(index - 1, long)
    }
  }

  override fun bindDouble(index: Int, double: Double?) {
    if (double == null) {
      statement.bindNull(index - 1, Double::class.java)
    } else {
      statement.bind(index - 1, double)
    }
  }

  override fun bindString(index: Int, string: String?) {
    if (string == null) {
      statement.bindNull(index - 1, String::class.java)
    } else {
      statement.bind(index - 1, string)
    }
  }

  override fun bindBoolean(index: Int, boolean: Boolean?) {
    if (boolean == null) {
      statement.bindNull(index - 1, Boolean::class.java)
    } else {
      statement.bind(index - 1, boolean)
    }
  }

  fun bindObject(index: Int, any: Any?) {
    if (any == null) {
      statement.bindNull(index - 1, Any::class.java)
    } else {
      statement.bind(index - 1, any)
    }
  }
}

/**
 * TODO: Write a better async cursor API
 */
open class R2dbcCursor(val rowSet: List<Map<Int, Any?>>) : SqlCursor {
  var row = -1
    private set

  override fun next(): Boolean = ++row < rowSet.size

  override fun getString(index: Int): String? = rowSet[row][index] as String?

  override fun getLong(index: Int): Long? = (rowSet[row][index] as Number?)?.toLong()

  override fun getBytes(index: Int): ByteArray? = rowSet[row][index] as ByteArray?

  override fun getDouble(index: Int): Double? = rowSet[row][index] as Double?

  override fun getBoolean(index: Int): Boolean? = rowSet[row][index] as Boolean?

  inline fun <reified T : Any> getObject(index: Int): T? = rowSet[row][index] as T?

  @Suppress("UNCHECKED_CAST")
  fun <T> getArray(index: Int): Array<T>? = rowSet[row][index] as Array<T>?
}

private fun <T> Publisher<T>.subscribe(
  next: (T) -> Unit = {},
  error: (Throwable) -> Unit = {},
  complete: () -> Unit = {},
) = subscribe(object : Subscriber<T> {
  private var subscription: Subscription? = null
  override fun onSubscribe(s: Subscription) {
    subscription = s
    s.request(Long.MAX_VALUE)
  }

  override fun onNext(t: T) {
    next(t)
  }

  override fun onError(t: Throwable) {
    error(t)
  }

  override fun onComplete() {
    complete()
    subscription?.cancel()
  }
})
