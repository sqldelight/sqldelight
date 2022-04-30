package app.cash.sqldelight.db

import app.cash.sqldelight.AsyncTransacter
import app.cash.sqldelight.Query
import app.cash.sqldelight.internal.Atomic
import app.cash.sqldelight.internal.AtomicBoolean
import app.cash.sqldelight.internal.getValue
import app.cash.sqldelight.internal.setValue

interface AsyncSqlDriver : Closeable {
  fun <R> executeQuery(
    identifier: Int?,
    sql: String,
    mapper: (SqlCursor) -> R,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)? = null,
  ): Callback<R>

  fun execute(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)? = null,
  ): Callback<Long>

  /**
   * Start a new [AsyncTransacter.Transaction] on the database.
   *
   * It's up to the implementor how this method behaves for different connection/threading patterns.
   */
  fun newTransaction(): Callback<out AsyncTransacter.Transaction>

  /**
   * The currently open [AsyncTransacter.Transaction] on the database.
   *
   * It's up to the implementor how this method behaves for different connection/threading patterns.
   */
  fun currentTransaction(): AsyncTransacter.Transaction?

  fun addListener(listener: Query.Listener, queryKeys: Array<String>)

  fun removeListener(listener: Query.Listener, queryKeys: Array<String>)

  fun notifyListeners(queryKeys: Array<String>)

  /**
   * API for creating and migrating a SQL database.
   */
  interface Schema {
    /**
     * The version of this schema.
     */
    val version: Int

    /**
     * Use [driver] to create the schema from scratch. Assumes no existing database state.
     */
    fun create(driver: AsyncSqlDriver): Callback<Unit>

    /**
     * Use [driver] to migrate from schema [oldVersion] to [newVersion].
     */
    fun migrate(driver: AsyncSqlDriver, oldVersion: Int, newVersion: Int): Callback<Unit>
  }

  /**
   * TODO: is design???
   */
  interface Callback<R> {
    fun onSuccess(block: (value: R) -> Unit): Callback<R>
    fun onError(block: (error: Throwable) -> Unit): Callback<R>
    fun start()
    val isComplete: Boolean
  }

  class SimpleCallback<R>(private val block: (SimpleCallback<R>) -> Unit) : Callback<R> {
    private val successCallbacks = mutableSetOf<(R) -> Unit>()
    private val errorCallbacks = mutableSetOf<(Throwable) -> Unit>()
    private var complete = false
    private var started by AtomicBoolean(false)

    override fun onSuccess(block: (value: R) -> Unit): Callback<R> = apply {
      successCallbacks.add(block)
    }

    override fun onError(block: (error: Throwable) -> Unit): Callback<R> = apply {
      errorCallbacks.add(block)
    }

    override fun start() {
      check(!started) { "Callback already started" }
      started = true
      try {
        block(this)
      } catch (e: Exception) {
        error(e)
      }
    }

    fun success(value: R) {
      complete = true
      successCallbacks.forEach { it.invoke(value) }
    }

    fun error(error: Throwable) {
      complete = true
      errorCallbacks.forEach { it.invoke(error) }
    }

    override val isComplete: Boolean
      get() = complete
  }
}

/**
 * Runs a list of [AsyncSqlDriver.Callback] and returns a callback that succeeds once all of the
 * callbacks have succeeded
 */
fun <R> List<AsyncSqlDriver.Callback<R>>.combine(): AsyncSqlDriver.Callback<Unit> {
  var counter by Atomic(0)
  val callback = AsyncSqlDriver.SimpleCallback<Unit> { callback ->
    forEach { inner ->
      inner.onSuccess {
        try {
          counter += 1
          if (counter == size) {
            callback.success(Unit)
          }
        } catch (e: Exception) {
          println(e)
          callback.error(e)
        }
      }.onError {
        callback.error(it)
        // Prevent success from being called again after a failure
        counter = size + 1
      }
      inner.start()
    }
  }
  return callback
}

fun <T, R> AsyncSqlDriver.Callback<T>.map(block: (T) -> R): AsyncSqlDriver.Callback<R> {
  val callback = AsyncSqlDriver.SimpleCallback<R> { callback ->
    this@map.onSuccess { callback.success(block(it)) }
    this@map.onError(callback::error)
    this@map.start()
  }
  return callback
}
