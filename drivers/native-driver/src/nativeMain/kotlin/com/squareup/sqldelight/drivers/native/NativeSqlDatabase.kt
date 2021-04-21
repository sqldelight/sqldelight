package com.squareup.sqldelight.drivers.native

import co.touchlab.sqliter.*
import co.touchlab.stately.concurrency.ThreadLocalRef
import co.touchlab.stately.concurrency.value
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.Closeable
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.drivers.native.util.PoolLock
import com.squareup.sqldelight.drivers.native.util.nativeCache
import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze

sealed class ConnectionWrapper : SqlDriver {
  internal abstract fun <R> accessConnection(
    readOnly: Boolean,
    block: ThreadConnection.() -> R
  ): R

  final override fun execute(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?
  ) {
    accessConnection(false) {
      val statement = useStatement(identifier, sql)
      if (binders != null) {
        try {
          SqliterStatement(statement).binders()
        } catch (t: Throwable) {
          statement.resetStatement()
          clearIfNeeded(identifier, statement)
          throw t
        }
      }

      statement.execute()
      statement.resetStatement()
      clearIfNeeded(identifier, statement)
    }
  }

  final override fun executeQuery(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?
  ): SqlCursor {
    return accessConnection(true) {
      val statement = getStatement(identifier, sql)

      if (binders != null) {
        try {
          SqliterStatement(statement).binders()
        } catch (t: Throwable) {
          statement.resetStatement()
          safePut(identifier, statement)
          throw t
        }
      }

      val cursor = statement.query()

      SqliterSqlCursor(cursor) {
        statement.resetStatement()
        if(closed)
          statement.finalizeStatement()
        safePut(identifier, statement)
      }
    }
  }
}

/**
 * Native driver implementation.
 *
 * The driver creates two connection pools, which default to 1 connection maximum. There is a reader pool, which
 * handles all query requests outside of a transaction. The other pool is the transaction pool, which handles
 * all transactions and write requests outside of a transaction.
 *
 * When a transaction is started, that thread is aligned with a transaction pool connection. Attempting a write or
 * starting another transaction, if no connections are available, will cause the caller to wait.
 *
 * You can have multiple connections in the transaction pool, but this would only be useful for read transactions. Writing
 * from multiple connections in an overlapping manner can be problematic.
 *
 * Aligning a transaction to a thread means you cannot operate on a single transaction from multiple threads.
 * However, it would be difficult to find a use case where this would be desirable or safe. Currently, the native
 * implementation of kotlinx.coroutines does not use thread pooling. When that changes, we'll need a way to handle
 * transaction/connection alignment similar to what the Android/JVM driver implemented.
 *
 * https://medium.com/androiddevelopers/threading-models-in-coroutines-and-android-sqlite-api-6cab11f7eb90
 *
 * To use SqlDelight during create/upgrade processes, you can alternatively wrap a real connection
 * with wrapConnection.
 *
 * SqlPreparedStatement instances also do not point to real resources until either execute or
 * executeQuery is called. The SqlPreparedStatement structure also maintains a thread-aligned
 * instance which accumulates bind calls. Those are replayed on a real SQLite statement instance
 * when execute or executeQuery is called. This avoids race conditions with bind calls.
 */
class NativeSqliteDriver(
  private val databaseManager: DatabaseManager,
  maxReaderConnections: Int = 1,
  maxTransactionConnections: Int = 1
) : ConnectionWrapper(), SqlDriver {
  internal val _maxTransactionConnections: Int = when {
    databaseManager.configuration.inMemory -> 1 //Memory db's are single connection, generally. You can use named connections, but there are other issues that need to be designed for
    databaseManager.configuration.journalMode == JournalMode.DELETE -> 1 //Multiple connections designed for WAL. Would need more effort to explicitly support other journal modes
    else -> maxTransactionConnections
  }

  internal val _maxReaderConnections: Int = when {
    databaseManager.configuration.inMemory -> 1 //Memory db's are single connection, generally. You can use named connections, but there are other issues that need to be designed for
    else -> maxReaderConnections
  }

  companion object {
    private const val NO_ID = -1
  }

  constructor(
    configuration: DatabaseConfiguration,
    maxReaderConnections: Int = 1,
    maxTransactionConnections: Int = 1
  ) : this(
    databaseManager = createDatabaseManager(configuration),
    maxTransactionConnections = maxTransactionConnections,
    maxReaderConnections = maxReaderConnections
  )

  constructor(
    schema: SqlDriver.Schema,
    name: String,
    maxReaderConnections: Int = 1,
    maxTransactionConnections: Int = 1
  ) : this(
    configuration = DatabaseConfiguration(
      name = name,
      version = schema.version,
      create = { connection ->
        wrapConnection(connection) { schema.create(it) }
      },
      upgrade = { connection, oldVersion, newVersion ->
        wrapConnection(connection) { schema.migrate(it, oldVersion, newVersion) }
      }
    ),
    maxTransactionConnections = maxTransactionConnections,
    maxReaderConnections = maxReaderConnections
  )

  // A pool of reader connections used by all operations not in a transaction
  private val transactionPool: Pool<ThreadConnection>
  private val readerPool: Pool<ThreadConnection>
  private val writeLock = PoolLock()

  // Once a transaction is started and connection borrowed, it will be here, but only for that
  // thread
  private val borrowedConnectionThread = ThreadLocalRef<Borrowed<ThreadConnection>>()

  internal val currentWriteConnId = AtomicInt(NO_ID)

  init {
    transactionPool = Pool(_maxTransactionConnections) {
      ThreadConnection(databaseManager.createMultiThreadedConnection()) { conn ->
        borrowedConnectionThread?.let {
          it.get()?.release()
          it.value = null
        }

        currentWriteConnId.compareAndSet(conn.connectionId, NO_ID)
        writeLock.notifyConditionChanged()
      }
    }
    readerPool = Pool(_maxReaderConnections) {
      val connection = databaseManager.createMultiThreadedConnection()
      connection.withStatement("PRAGMA query_only = 1") { execute() } //Ensure read only
      ThreadConnection(connection) {
        throw UnsupportedOperationException("Should never be in a transaction")
      }
    }
  }

  override fun currentTransaction(): Transacter.Transaction? {
    return borrowedConnectionThread.get()?.value?.transaction?.value
  }

  override fun newTransaction(): Transacter.Transaction {
    val alreadyBorrowed = borrowedConnectionThread.get()
    return if (alreadyBorrowed == null) {
      val borrowed = transactionPool.borrowEntry()

      try {
        val trans = borrowed.value.newTransaction()

        borrowedConnectionThread.value = borrowed
        trans
      } catch (e: Throwable) {
        // Unlock on failure.
        borrowed.release()
        throw e
      }
    } else {
      alreadyBorrowed.value.newTransaction()
    }
  }

  /**
   * If we're in a transaction, then I have a connection. Otherwise use shared.
   */
  override fun <R> accessConnection(
    readOnly: Boolean,
    block: ThreadConnection.() -> R
  ): R {
    val mine = borrowedConnectionThread.get()

    return if (readOnly) {
      //Code intends to read, which doesn't need to block
      if (mine != null) {
        mine.value.block()
      } else {
        readerPool.access(block)
      }
    } else {
      //Code intends to write, for which we're managing locks in code
      if (mine != null) {
        val id = mine.value.connectionId
        writeLock.withLock {
          loopUntilConditionalResult { currentWriteConnId.value == id || currentWriteConnId.compareAndSet(NO_ID, id) }
          mine.value.block()
        }
      } else {
        writeLock.withLock {
          loopUntilConditionalResult { currentWriteConnId.value == NO_ID }
          transactionPool.access(block)
        }
      }
    }
  }

  override fun close() {
    transactionPool.close()
    readerPool.close()
  }
}

/**
 * Sqliter's DatabaseConfiguration takes lambda arguments for it's create and upgrade operations,
 * which each take a DatabaseConnection argument. Use wrapConnection to have SqlDelight access this
 * passed connection and avoid the pooling that the full SqlDriver instance performs.
 *
 * Note that queries created during this operation will be cleaned up. If holding onto a cursor from
 * a wrap call, it will no longer be viable.
 */
fun wrapConnection(
  connection: DatabaseConnection,
  block: (SqlDriver) -> Unit
) {
  val conn = SqliterWrappedConnection(ThreadConnection(connection) {})
  try {
    block(conn)
  } finally {
    conn.close()
  }
}

/**
 * SqlDriverConnection that wraps a Sqliter connection. Useful for migration tasks, or if you
 * don't want the polling.
 */
internal class SqliterWrappedConnection(
  private val threadConnection: ThreadConnection
) : ConnectionWrapper(),
  SqlDriver {
  override fun currentTransaction(): Transacter.Transaction? = threadConnection.transaction.value

  override fun newTransaction(): Transacter.Transaction = threadConnection.newTransaction()

  override fun <R> accessConnection(
    readOnly: Boolean,
    block: ThreadConnection.() -> R
  ): R = threadConnection.block()

  override fun close() {
    threadConnection.cleanUp()
  }
}

/**
 * Wraps and manages a "real" database connection.
 *
 * SQLite statements are specific to connections, and must be finalized explicitly. Cursors are
 * backed by a statement resource, so we keep links to open cursors to allow us to close them out
 * properly in cases where the user does not.
 */
internal class ThreadConnection(
  private val connection: DatabaseConnection,
  private val onEndTransaction: (ThreadConnection)->Unit
) : Closeable {

  companion object {
    private val idCounter = AtomicInt(0)
  }

  internal val transaction: AtomicReference<Transacter.Transaction?> = AtomicReference(null)
  internal val closed:Boolean
    get() = connection.closed

  internal val statementCache = nativeCache<Statement>()
  internal val connectionId: Int = idCounter.addAndGet(1)

  fun safePut(identifier: Int?, statement: Statement) {
    val removed = if (identifier == null) {
      statement
    } else {
      statementCache.put(identifier.toString(), statement)
    }
    removed?.finalizeStatement()
  }

  fun getStatement(identifier: Int?, sql: String): Statement {
    val statement = removeCreateStatement(identifier, sql)
    return statement
  }

  fun useStatement(identifier: Int?, sql: String): Statement {
    return if (identifier != null) {
      statementCache.getOrCreate(identifier.toString()) {
        connection.createStatement(sql)
      }
    } else {
      connection.createStatement(sql)
    }
  }

  fun clearIfNeeded(identifier: Int?, statement: Statement){
    if(identifier == null){
      statement.finalizeStatement()
    }
  }

  /**
   * For cursors. Cursors are actually backed by SQLite statement instances, so they need to be
   * removed from the cache when in use. We're giving out a SQLite resource here, so extra care.
   */
  private fun removeCreateStatement(identifier: Int?, sql: String): Statement {
    if (identifier != null) {
      val cached = statementCache.remove(identifier.toString())
      if (cached != null)
        return cached
    }

    return connection.createStatement(sql)
  }

  fun newTransaction(): Transacter.Transaction {
    val enclosing = transaction.value

    // Create here, in case we bomb...
    if (enclosing == null) {
      connection.beginTransaction()
    }

    val trans = Transaction(enclosing).freeze()
    transaction.value = trans

    return trans
  }

  /**
   * This should only be called directly from wrapConnection. Clean resources without actually closing
   * the underlying connection.
   */
  internal fun cleanUp() {
    statementCache.cleanUp {
      it.finalizeStatement()
    }
  }

  override fun close() {
    cleanUp()
    connection.close()
  }

  private inner class Transaction(
    override val enclosingTransaction: Transacter.Transaction?
  ) : Transacter.Transaction() {
    override fun endTransaction(successful: Boolean) {
      // This stays here to avoid a race condition with multiple threads and transactions
      transaction.value = enclosingTransaction.freeze()

      if (enclosingTransaction == null) {
        try {
          if (successful) {
            connection.setTransactionSuccessful()
          }

          connection.endTransaction()
        } finally {
          // Release if we have
          onEndTransaction(this@ThreadConnection)
        }
      }
    }
  }
}
