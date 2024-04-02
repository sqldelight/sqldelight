package app.cash.sqldelight.driver.native

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.Closeable
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.util.PoolLock
import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.DatabaseConnection
import co.touchlab.sqliter.DatabaseManager
import co.touchlab.sqliter.Statement
import co.touchlab.sqliter.createDatabaseManager
import co.touchlab.sqliter.withStatement
import co.touchlab.stately.concurrency.ThreadLocalRef
import co.touchlab.stately.concurrency.value

sealed class ConnectionWrapper : SqlDriver {
  internal abstract fun <R> accessConnection(
    readOnly: Boolean,
    block: ThreadConnection.() -> R,
  ): R

  private fun <R> accessStatement(
    readOnly: Boolean,
    identifier: Int?,
    sql: String,
    binders: (SqlPreparedStatement.() -> Unit)?,
    block: (Statement) -> R,
  ): R {
    return accessConnection(readOnly) {
      val statement = useStatement(identifier, sql)
      try {
        if (binders != null) {
          SqliterStatement(statement).binders()
        }

        block(statement)
      } finally {
        statement.resetStatement()
        clearIfNeeded(identifier, statement)
      }
    }
  }

  final override fun execute(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?,
  ): QueryResult<Long> = QueryResult.Value(
    accessStatement(false, identifier, sql, binders) { statement ->
      statement.executeUpdateDelete().toLong()
    },
  )

  final override fun <R> executeQuery(
    identifier: Int?,
    sql: String,
    mapper: (SqlCursor) -> QueryResult<R>,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?,
  ): QueryResult<R> = accessStatement(true, identifier, sql, binders) { statement ->
    mapper(SqliterSqlCursor(statement.query()))
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
    maxReaderConnections: Int = 1,
  ) : this(
    databaseManager = createDatabaseManager(configuration),
    maxReaderConnections = maxReaderConnections,
  )

  /**
   * @param onConfiguration Callback to hook into [DatabaseConfiguration] creation.
   */
  constructor(
    schema: SqlSchema<QueryResult.Value<Unit>>,
    name: String,
    maxReaderConnections: Int = 1,
    onConfiguration: (DatabaseConfiguration) -> DatabaseConfiguration = { it },
    vararg callbacks: AfterVersion,
  ) : this(
    configuration = DatabaseConfiguration(
      name = name,
      version = if (schema.version > Int.MAX_VALUE) error("Schema version is larger than Int.MAX_VALUE: ${schema.version}.") else schema.version.toInt(),
      create = { connection -> wrapConnection(connection) { schema.create(it) } },
      upgrade = { connection, oldVersion, newVersion ->
        wrapConnection(connection) { schema.migrate(it, oldVersion.toLong(), newVersion.toLong(), *callbacks) }
      },
    ).let(onConfiguration),
    maxReaderConnections = maxReaderConnections,
  )

  // A pool of reader connections used by all operations not in a transaction
  private val transactionPool: Pool<ThreadConnection>
  internal val readerPool: Pool<ThreadConnection>

  // Once a transaction is started and connection borrowed, it will be here, but only for that
  // thread
  private val borrowedConnectionThread = ThreadLocalRef<Borrowed<ThreadConnection>>()
  private val listeners = mutableMapOf<String, MutableSet<Query.Listener>>()
  private val lock = PoolLock(reentrant = true)

  init {
    if (databaseManager.configuration.isEphemeral) {
      // Single connection for transactions
      transactionPool = Pool(1) {
        ThreadConnection(databaseManager.createMultiThreadedConnection()) { _ ->
          borrowedConnectionThread.let {
            it.get()?.release()
            it.value = null
          }
        }
      }

      readerPool = transactionPool
    } else {
      // Single connection for transactions
      transactionPool = Pool(1) {
        ThreadConnection(databaseManager.createMultiThreadedConnection()) { _ ->
          borrowedConnectionThread.let {
            it.get()?.release()
            it.value = null
          }
        }
      }

      readerPool = Pool(maxReaderConnections) {
        val connection = databaseManager.createMultiThreadedConnection()
        connection.withStatement("PRAGMA query_only = 1") { execute() } // Ensure read only
        ThreadConnection(connection) {
          throw UnsupportedOperationException("Should never be in a transaction")
        }
      }
    }
  }

  override fun addListener(vararg queryKeys: String, listener: Query.Listener) {
    lock.withLock {
      queryKeys.forEach {
        listeners.getOrPut(it) { mutableSetOf() }.add(listener)
      }
    }
  }

  override fun removeListener(vararg queryKeys: String, listener: Query.Listener) {
    lock.withLock {
      queryKeys.forEach {
        listeners.get(it)?.remove(listener)
      }
    }
  }

  override fun notifyListeners(vararg queryKeys: String) {
    val listenersToNotify = mutableSetOf<Query.Listener>()
    lock.withLock {
      queryKeys.forEach { key -> listeners.get(key)?.let { listenersToNotify.addAll(it) } }
    }
    listenersToNotify.forEach(Query.Listener::queryResultsChanged)
  }

  override fun currentTransaction(): Transacter.Transaction? {
    return borrowedConnectionThread.get()?.value?.transaction?.value
  }

  override fun newTransaction(): QueryResult<Transacter.Transaction> {
    val alreadyBorrowed = borrowedConnectionThread.get()
    val transaction = if (alreadyBorrowed == null) {
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

    return QueryResult.Value(transaction)
  }

  /**
   * If we're in a transaction, then I have a connection. Otherwise use shared.
   */
  override fun <R> accessConnection(
    readOnly: Boolean,
    block: ThreadConnection.() -> R,
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
 * Helper function to create an in-memory driver. In-memory drivers have a single connection, so
 * concurrent access will be block
 */
fun inMemoryDriver(schema: SqlSchema<QueryResult.Value<Unit>>): NativeSqliteDriver = NativeSqliteDriver(
  DatabaseConfiguration(
    name = null,
    inMemory = true,
    version = if (schema.version > Int.MAX_VALUE) error("Schema version is larger than Int.MAX_VALUE: ${schema.version}.") else schema.version.toInt(),
    create = { connection ->
      wrapConnection(connection) { schema.create(it) }
    },
    upgrade = { connection, oldVersion, newVersion ->
      wrapConnection(connection) { schema.migrate(it, oldVersion.toLong(), newVersion.toLong()) }
    },
  ),
)

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
  block: (SqlDriver) -> Unit,
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
  private val threadConnection: ThreadConnection,
) : ConnectionWrapper(),
  SqlDriver {
  override fun currentTransaction(): Transacter.Transaction? = threadConnection.transaction.value

  override fun newTransaction(): QueryResult<Transacter.Transaction> = QueryResult.Value(threadConnection.newTransaction())

  override fun <R> accessConnection(
    readOnly: Boolean,
    block: ThreadConnection.() -> R,
  ): R = threadConnection.block()

  override fun addListener(vararg queryKeys: String, listener: Query.Listener) {
    // No-op
  }

  override fun removeListener(vararg queryKeys: String, listener: Query.Listener) {
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
  private val onEndTransaction: (ThreadConnection) -> Unit,
) : Closeable {
  internal val transaction = ThreadLocalRef<Transacter.Transaction?>()
  private val closed: Boolean
    get() = connection.closed

  private val statementCache = mutableMapOf<Int, Statement>()

  fun useStatement(identifier: Int?, sql: String): Statement {
    return if (identifier != null) {
      statementCache.getOrPut(identifier) {
        connection.createStatement(sql)
      }
    } else {
      connection.createStatement(sql)
    }
  }

  fun clearIfNeeded(identifier: Int?, statement: Statement) {
    if (identifier == null || closed) {
      statement.finalizeStatement()
    }
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
    statementCache.values.forEach { it: Statement ->
      it.finalizeStatement()
    }
  }

  override fun close() {
    cleanUp()
    connection.close()
  }

  private inner class Transaction(
    override val enclosingTransaction: Transacter.Transaction?,
  ) : Transacter.Transaction() {

    override fun endTransaction(successful: Boolean): QueryResult<Unit> {
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
      return QueryResult.Unit
    }
  }
}

private inline val DatabaseConfiguration.isEphemeral: Boolean get() {
  return inMemory || (name?.isEmpty() == true && extendedConfig.basePath?.isEmpty() == true)
}
