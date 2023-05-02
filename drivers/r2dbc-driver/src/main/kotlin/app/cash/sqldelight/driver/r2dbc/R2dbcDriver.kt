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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import kotlin.coroutines.*
import kotlin.reflect.KClass

class R2dbcDriver(
  val connection: Connection,
  val onClose: () -> Unit,
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
      val resultFlow = result.map { row, _ -> row }.asFlow()
        //.buffer(capacity = 1, onBufferOverflow = BufferOverflow.SUSPEND,)
      
      mapper(R2dbcCursor(resultFlow.iterator())).await()
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

  override fun addListener(listener: Query.Listener, queryKeys: Array<String>) = Unit
  override fun removeListener(listener: Query.Listener, queryKeys: Array<String>) = Unit
  override fun notifyListeners(queryKeys: Array<String>) = Unit

  override fun close() {
    connection.close().subscribe(
      object : Subscriber<Void> {
        override fun onSubscribe(s: Subscription) {
          s.request(1)
        }

        override fun onError(t: Throwable) {
          throw t
        }

        override fun onComplete() {
          onClose()
        }

        override fun onNext(t: Void) {
          error("Should not be called during connection.close, error in R2DBC implementation.")
        }
      },
    )
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

internal interface FlowIterator<T: Any> {
 suspend fun next(): T?
}

internal fun <T : Any> Flow<T>.iterator(): FlowIterator<T> = object : FlowIterator<T> {
  private var next: IteratorResult = IteratorResult.Initial
  private var collectionCont: CancellableContinuation<CancellableContinuation<ContToken<T>>>? = null
  var collectorJob: Job? = null
  private var iteratorJob: Job? = null

  override suspend fun next(): T? = coroutineScope {
    if (next is IteratorResult.Initial || next is IteratorResult.Success<*>) {
      if (iteratorJob == null) {
        iteratorJob = coroutineContext[Job]
      } else {
        check(iteratorJob === coroutineContext[Job]) {
          "FlowIterator is not thread-safe and cannot be used from multiple coroutines."
        }
      }
      val (theNext, theCollectionCont) = suspendCancellableCoroutine { tokenCont ->
        collectionCont?.invokeOnCancellation {
          collectorJob?.cancel("Canceled eagerly")
        }
        when (val theCollectionCont = collectionCont) {
          null -> collectorJob = launch { doCollect(tokenCont) }
          else -> theCollectionCont.resume(tokenCont)
        }
      }
      next = theNext
      collectionCont = theCollectionCont
    }

    when (val nextValue = next) {
      IteratorResult.Closed -> null
      is IteratorResult.Error -> throw nextValue.error
      IteratorResult.Initial -> error("Internal Cursor error, Initial not set.")
      is IteratorResult.Success<*> -> {
        @Suppress("UNCHECKED_CAST")
        val r = nextValue.element as T
        next = IteratorResult.Initial
        r
      }
    }
  }

  private suspend fun doCollect(firstTokenCont: CancellableContinuation<ContToken<T>>) {
    var tokenCont = firstTokenCont
    onCompletion { thrown ->
      if (thrown == null) {
        tokenCont = suspendCancellableCoroutine { collectionCont ->
          tokenCont.resume(
            ContToken(
              IteratorResult.Closed,
              collectionCont
            )
          )
        }
      } else if (thrown !is CancellationException) {
        // should never get used
        tokenCont = suspendCancellableCoroutine { collectionCont ->
          tokenCont.resume(
            ContToken(
              IteratorResult.Error(thrown),
              collectionCont
            )
          )
        }
      }
    }.collect { elem ->
      tokenCont = suspendCancellableCoroutine { collectionCont ->
        tokenCont.resume(ContToken(IteratorResult.Success(elem), collectionCont))
      }
    }
  }
}

private sealed interface IteratorResult {
  object Initial: IteratorResult
  @JvmInline
  value class Success<E: Any>(val element: E): IteratorResult
  @JvmInline
  value class Error(val error: Throwable): IteratorResult
  object Closed: IteratorResult
}

private data class ContToken<T>(
  val nextValue: IteratorResult,
  val resumption: CancellableContinuation<CancellableContinuation<ContToken<T>>>
)

class R2dbcCursor internal constructor(
  private val rows: FlowIterator<Row>,
) : SqlCursor {
  private lateinit var currentRow: Row

  override fun next(): QueryResult.AsyncValue<Boolean> = QueryResult.AsyncValue {
    val next = rows.next() ?: return@AsyncValue false
    currentRow = next
    true
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
