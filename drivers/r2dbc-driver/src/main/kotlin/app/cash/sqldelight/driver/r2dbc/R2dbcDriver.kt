package app.cash.sqldelight.driver.r2dbc

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import io.r2dbc.spi.Connection
import io.r2dbc.spi.Row
import io.r2dbc.spi.Statement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelIterator
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import kotlin.reflect.KClass

class R2dbcDriver(
  val connection: Connection,
  /**
   * This callback is called after [close]. It either contains an error or null, representing a successful close.
   */
  val closed: (Throwable?) -> Unit = { },
) : SqlDriver {
  override fun <R> executeQuery(
    identifier: Int?,
    sql: String,
    mapper: (SqlCursor) -> QueryResult<R>,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?,
  ): QueryResult<R> {
    val prepared = connection.createStatement(sql).also { statement ->
      R2dbcPreparedStatement(statement).apply { if (binders != null) this.binders() }
    }

    return QueryResult.AsyncValue {
      val result = prepared.execute().awaitSingle()
      val resultChannel = result.map { row, _ -> row }
      mapper(R2dbcCursor(resultChannel.iterator())).await()
    }
  }

  override fun execute(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?,
  ): QueryResult<Long> {
    val prepared = connection.createStatement(sql).also { statement ->
      R2dbcPreparedStatement(statement).apply { if (binders != null) this.binders() }
    }

    return QueryResult.AsyncValue {
      val result = prepared.execute().awaitSingle()
      return@AsyncValue result.rowsUpdated.awaitFirstOrNull() ?: 0L
    }
  }

  private val transactions = ThreadLocal<Transacter.Transaction>()
  private var transaction: Transacter.Transaction?
    get() = transactions.get()
    set(value) {
      transactions.set(value)
    }

  override fun newTransaction(): QueryResult<Transacter.Transaction> = QueryResult.AsyncValue {
    val enclosing = transaction
    val transaction = Transaction(enclosing, connection)
    this.transaction = transaction

    if (enclosing == null) {
      connection.beginTransaction().awaitFirstOrNull()
    }

    return@AsyncValue transaction
  }

  override fun currentTransaction(): Transacter.Transaction? = transaction

  override fun addListener(vararg queryKeys: String, listener: Query.Listener) = Unit
  override fun removeListener(vararg queryKeys: String, listener: Query.Listener) = Unit
  override fun notifyListeners(vararg queryKeys: String) = Unit

  override fun close() {
    // Normally, this is just a Mono, so it completes directly without onNext.
    // But the standard allows any publisher, so we should request unlimited items
    // and wait until the close call is finally completed.
    connection.close().subscribe(object : Subscriber<Void> {
      override fun onSubscribe(sub: Subscription) {
        sub.request(Long.MAX_VALUE)
      }

      override fun onError(error: Throwable) {
        closed(error)
      }

      override fun onComplete() {
        closed(null)
      }

      override fun onNext(t: Void) {
        // Do nothing, we wait until completion.
      }
    })
  }

  private inner class Transaction(
    override val enclosingTransaction: Transacter.Transaction?,
    private val connection: Connection,
  ) : Transacter.Transaction() {
    override fun endTransaction(successful: Boolean): QueryResult<Unit> = QueryResult.AsyncValue {
      if (enclosingTransaction == null) {
        if (successful) {
          connection.commitTransaction().awaitFirstOrNull()
        } else {
          connection.rollbackTransaction().awaitFirstOrNull()
        }
      }
      transaction = enclosingTransaction
    }
  }
}

/**
 * Creates and returns a [R2dbcDriver] with the given [connection].
 *
 * The scope waits until the driver is closed [R2dbcDriver.close].
 */
fun CoroutineScope.R2dbcDriver(
  connection: Connection,
): R2dbcDriver {
  val completed = Job()
  val driver = R2dbcDriver(connection) {
    if (it == null) {
      completed.complete()
    } else {
      completed.completeExceptionally(it)
    }
  }
  launch {
    completed.join()
  }
  return driver
}

// R2DBC uses boxed Java classes instead primitives: https://r2dbc.io/spec/1.0.0.RELEASE/spec/html/#datatypes
class R2dbcPreparedStatement(private val statement: Statement) : SqlPreparedStatement {
  override fun bindBytes(index: Int, bytes: ByteArray?) {
    if (bytes == null) {
      statement.bindNull(index, ByteArray::class.java)
    } else {
      statement.bind(index, bytes)
    }
  }

  override fun bindLong(index: Int, long: Long?) {
    if (long == null) {
      statement.bindNull(index, Long::class.javaObjectType)
    } else {
      statement.bind(index, long)
    }
  }

  override fun bindDouble(index: Int, double: Double?) {
    if (double == null) {
      statement.bindNull(index, Double::class.javaObjectType)
    } else {
      statement.bind(index, double)
    }
  }

  override fun bindString(index: Int, string: String?) {
    if (string == null) {
      statement.bindNull(index, String::class.java)
    } else {
      statement.bind(index, string)
    }
  }

  override fun bindBoolean(index: Int, boolean: Boolean?) {
    if (boolean == null) {
      statement.bindNull(index, Boolean::class.javaObjectType)
    } else {
      statement.bind(index, boolean)
    }
  }

  fun bindObject(index: Int, any: Any?) {
    if (any == null) {
      statement.bindNull(index, Any::class.java)
    } else {
      statement.bind(index, any)
    }
  }
}

internal fun<T : Any> Publisher<T>.iterator(): ChannelIterator<T> = AsyncChannelIterator(this)

private class AsyncChannelIterator<T : Any>(
  pub: Publisher<T>,
) : ChannelIterator<T> {
  private val syncChannel: Channel<T?> = Channel(
    capacity = 1,
    onBufferOverflow = BufferOverflow.SUSPEND,
  )
  private val subscription = CompletableDeferred<Subscription>()
  private lateinit var next: T

  init {
    pub.subscribe(object : Subscriber<T> {
      private lateinit var sub: Subscription
      override fun onSubscribe(sub: Subscription) {
        println("onSubscribe")
        this.sub = sub
        subscription.complete(sub)
      }

      override fun onError(error: Throwable) {
        println("onError $error")
        syncChannel.close(error)
      }

      override fun onComplete() {
        println("onComplete")
        syncChannel.close()
      }

      override fun onNext(nextValue: T) {
        println("onNext $nextValue")
        syncChannel.trySendBlocking(nextValue)
      }
    })
  }

  override suspend fun hasNext(): Boolean {
    println("hasNext")
    val sub = subscription.await()
    sub.request(1)
    try {
      val next = syncChannel.receiveCatching().getOrNull().also {
        println("hasNext received $it")
      } ?: return false
      this.next = next
      return true
    } catch (cancel: CancellationException) {
      sub.cancel()
      throw cancel
    }
  }

  override fun next(): T = next
}

class R2dbcCursor
internal constructor(private val results: ChannelIterator<Row>) : SqlCursor {
  private lateinit var currentRow: Row
  override fun next(): QueryResult.AsyncValue<Boolean> = QueryResult.AsyncValue {
    val hasNext = results.hasNext()
    if (hasNext) {
      currentRow = results.next()
      true
    } else {
      false
    }
  }

  @PublishedApi
  internal fun <T : Any> get(index: Int, klass: KClass<T>): T? {
    return currentRow.get(index, klass.java)
  }

  override fun getString(index: Int): String? = get(index, String::class)

  override fun getLong(index: Int): Long? = get(index, Number::class)?.toLong()

  override fun getBytes(index: Int): ByteArray? = get(index, ByteArray::class)

  override fun getDouble(index: Int): Double? = get(index, Double::class)

  override fun getBoolean(index: Int): Boolean? = get(index, Boolean::class)

  inline fun <reified T : Any> getObject(index: Int): T? = get(index, T::class)

  @Suppress("UNCHECKED_CAST")
  fun <T> getArray(index: Int): Array<T>? = get(index, Array::class) as Array<T>?
}
