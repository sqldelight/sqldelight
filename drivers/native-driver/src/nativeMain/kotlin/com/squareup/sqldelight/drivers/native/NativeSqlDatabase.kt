package com.squareup.sqldelight.drivers.native

import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.DatabaseConnection
import co.touchlab.sqliter.DatabaseManager
import co.touchlab.sqliter.Statement
import co.touchlab.sqliter.createDatabaseManager
import co.touchlab.sqliter.withStatement
import co.touchlab.stately.collections.SharedHashMap
import co.touchlab.stately.collections.SharedSet
import co.touchlab.stately.concurrency.ThreadLocalRef
import co.touchlab.stately.concurrency.value
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.Closeable
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.drivers.native.util.nativeCache
import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.ensureNeverFrozen

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
        if (closed)
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
) : ConnectionWrapper(), SqlDriver {
  constructor(
    configuration: DatabaseConfiguration,
    maxReaderConnections: Int = 1
  ) : this(
    databaseManager = createDatabaseManager(configuration),
    maxReaderConnections = maxReaderConnections
  )

  constructor(
    schema: SqlDriver.Schema,
    name: String,
    maxReaderConnections: Int = 1
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
    maxReaderConnections = maxReaderConnections
  )

  // A pool of reader connections used by all operations not in a transaction
  internal val transactionPool: Pool<ThreadConnection>
  internal val readerPool: Pool<ThreadConnection>

  // Once a transaction is started and connection borrowed, it will be here, but only for that
  // thread
  private val borrowedConnectionThread = ThreadLocalRef<Borrowed<ThreadConnection>>()
  private val listeners = SharedHashMap<String, SharedSet<Query.Listener>>()

  init {
    // Single connection for transactions
    transactionPool = Pool(1) {
      ThreadConnection(databaseManager.createMultiThreadedConnection()) { conn ->
        borrowedConnectionThread.let {
          it.get()?.release()
          it.value = null
        }
      }
    }

    val maxReaderConnectionsForConfig: Int = when {
      databaseManager.configuration.inMemory -> 1 // Memory db's are single connection, generally. You can use named connections, but there are other issues that need to be designed for
      else -> maxReaderConnections
    }
    readerPool = Pool(maxReaderConnectionsForConfig) {
      val connection = databaseManager.createMultiThreadedConnection()
      connection.withStatement("PRAGMA query_only = 1") { execute() } // Ensure read only
      ThreadConnection(connection) {
        throw UnsupportedOperationException("Should never be in a transaction")
      }
    }
  }

  override fun addListener(listener: Query.Listener, vararg queryKeys: String) {
    queryKeys.forEach {
      listeners.getOrPut(it, { SharedSet() }).add(listener)
    }
  }

  override fun removeListener(listener: Query.Listener, vararg queryKeys: String) {
    queryKeys.forEach {
      listeners[it]?.remove(listener)
    }
  }

  override fun notifyListeners(vararg queryKeys: String) {
    queryKeys.flatMap { listeners[it].orEmpty() }
      .distinct()
      .forEach(Query.Listener::queryResultsChanged)
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
      // Code intends to read, which doesn't need to block
      if (mine != null) {
        mine.value.block()
      } else {
        readerPool.access(block)
      }
    } else {
      // Code intends to write, for which we're managing locks in code
      if (mine != null) {
        mine.value.block()
      } else {
        transactionPool.access(block)
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

  override fun addListener(listener: Query.Listener, vararg queryKeys: String) {
    // No-op
  }

  override fun removeListener(listener: Query.Listener, vararg queryKeys: String) {
    // No-op
  }

  override fun notifyListeners(vararg queryKeys: String) {
    // No-op
  }

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
  private val onEndTransaction: (ThreadConnection) -> Unit
) : Closeable {

  companion object {
    private val idCounter = AtomicInt(0)
  }

  internal val transaction = ThreadLocalRef<Transacter.Transaction?>()
  internal val closed: Boolean
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

  fun clearIfNeeded(identifier: Int?, statement: Statement) {
    if (identifier == null) {
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

    val trans = Transaction(enclosing)
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
    init { ensureNeverFrozen() }

    override fun endTransaction(successful: Boolean) {
      transaction.value = enclosingTransaction

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
