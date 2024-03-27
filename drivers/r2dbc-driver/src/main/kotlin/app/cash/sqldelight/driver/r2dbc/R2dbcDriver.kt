package app.cash.sqldelight.driver.r2dbc

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import io.r2dbc.spi.Connection
import io.r2dbc.spi.Statement
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.intellij.lang.annotations.Language
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

class R2dbcDriver(
  val connection: Connection,
  /**
   * This callback is called after [close]. It either contains an error or null, representing a successful close.
   */
  val closed: (Throwable?) -> Unit = { },
) : SqlDriver {
  override fun <R> executeQuery(
    identifier: Int?,
    @Language("SQL") sql: String,
    mapper: (SqlCursor) -> QueryResult<R>,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?,
  ): QueryResult<R> {
    val prepared = connection.createStatement(sql).also { statement ->
      R2dbcPreparedStatement(statement).apply { if (binders != null) this.binders() }
    }

    return QueryResult.AsyncValue {
      val result = prepared.execute().awaitSingle()

      val rowPublisher = result.map { row, rowMetadata ->
        List(rowMetadata.columnMetadatas.size) { index ->
          row.get(index)
        }
      }

      return@AsyncValue mapper(R2dbcCursor(rowPublisher.asIterator())).await()
    }
  }

  override fun execute(
    identifier: Int?,
    @Language("SQL") sql: String,
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
class R2dbcPreparedStatement(val statement: Statement) : SqlPreparedStatement {
  override fun bindBytes(index: Int, bytes: ByteArray?) {
    if (bytes == null) {
      statement.bindNull(index, ByteArray::class.java)
    } else {
      statement.bind(index, bytes)
    }
  }

  override fun bindBoolean(index: Int, boolean: Boolean?) {
    if (boolean == null) {
      statement.bindNull(index, Boolean::class.javaObjectType)
    } else {
      statement.bind(index, boolean)
    }
  }

  fun bindByte(index: Int, byte: Byte?) {
    if (byte == null) {
      statement.bindNull(index, Byte::class.javaObjectType)
    } else {
      statement.bind(index, byte)
    }
  }

  fun bindShort(index: Int, short: Short?) {
    if (short == null) {
      statement.bindNull(index, Short::class.javaObjectType)
    } else {
      statement.bind(index, short)
    }
  }

  fun bindInt(index: Int, int: Int?) {
    if (int == null) {
      statement.bindNull(index, Int::class.javaObjectType)
    } else {
      statement.bind(index, int)
    }
  }

  override fun bindLong(index: Int, long: Long?) {
    if (long == null) {
      statement.bindNull(index, Long::class.javaObjectType)
    } else {
      statement.bind(index, long)
    }
  }

  fun bindFloat(index: Int, float: Float?) {
    if (float == null) {
      statement.bindNull(index, Float::class.javaObjectType)
    } else {
      statement.bind(index, float)
    }
  }

  override fun bindDouble(index: Int, double: Double?) {
    if (double == null) {
      statement.bindNull(index, Double::class.javaObjectType)
    } else {
      statement.bind(index, double)
    }
  }

  fun bindBigDecimal(index: Int, decimal: BigDecimal?) {
    if (decimal == null) {
      statement.bindNull(index, BigDecimal::class.java)
    } else {
      statement.bind(index, decimal)
    }
  }

  fun bindObject(index: Int, any: Any?, ignoredSqlType: Int = 0) {
    if (any == null) {
      statement.bindNull(index, Any::class.java)
    } else {
      statement.bind(index, any)
    }
  }

  @JvmName("bindTypedObject")
  inline fun <reified T : Any> bindObject(index: Int, any: T?) {
    if (any == null) {
      statement.bindNull(index, T::class.java)
    } else {
      statement.bind(index, any)
    }
  }

  override fun bindString(index: Int, string: String?) {
    if (string == null) {
      statement.bindNull(index, String::class.java)
    } else {
      statement.bind(index, string)
    }
  }
}

internal fun <T : Any> Publisher<T>.asIterator(): AsyncPublisherIterator<T> =
  AsyncPublisherIterator(this)

internal class AsyncPublisherIterator<T : Any>(
  pub: Publisher<T>,
) {
  private var nextValue = CompletableDeferred<T?>()
  private val subscription = CompletableDeferred<Subscription>()

  init {
    pub.subscribe(object : Subscriber<T> {
      override fun onSubscribe(sub: Subscription) {
        subscription.complete(sub)
      }

      override fun onError(error: Throwable) {
        nextValue.completeExceptionally(error)
      }

      override fun onComplete() {
        nextValue.complete(null)
      }

      override fun onNext(next: T) {
        nextValue.complete(next)
      }
    })
  }

  suspend fun next(): T? {
    val sub = subscription.await()
    sub.request(1)
    try {
      val next = nextValue.await() ?: return null
      nextValue = CompletableDeferred()
      return next
    } catch (cancel: CancellationException) {
      sub.cancel()
      throw cancel
    }
  }
}

class R2dbcCursor
internal constructor(private val results: AsyncPublisherIterator<List<Any?>>) : SqlCursor {
  private lateinit var currentRow: List<Any?>

  override fun next(): QueryResult.AsyncValue<Boolean> = QueryResult.AsyncValue {
    val next = results.next() ?: return@AsyncValue false
    currentRow = next
    true
  }

  @PublishedApi
  internal fun <T : Any> get(index: Int): T? {
    @Suppress("UNCHECKED_CAST")
    return currentRow[index] as T?
  }

  override fun getString(index: Int): String? = get(index)
  fun getShort(index: Int): Short? = get<Number>(index)?.toShort()
  fun getInt(index: Int): Int? = get<Number>(index)?.toInt()

  override fun getLong(index: Int): Long? = get<Number>(index)?.toLong()

  override fun getBytes(index: Int): ByteArray? = get(index)

  override fun getDouble(index: Int): Double? = get(index)

  override fun getBoolean(index: Int): Boolean? = get(index)

  inline fun <reified T : Any> getObject(index: Int): T? = get(index)

  fun <T> getArray(index: Int): Array<T>? = get(index)
}
