package com.squareup.sqldelight.drivers.native

import co.touchlab.sqliter.*
import co.touchlab.stately.concurrency.AtomicReference
import co.touchlab.stately.concurrency.ThreadLocalRef
import co.touchlab.stately.concurrency.value
import co.touchlab.stately.freeze
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.Closeable
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.drivers.native.util.NativeCache
import com.squareup.sqldelight.drivers.native.util.cleanUp

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

      statement.execute()
      statement.resetStatement()
      safePut(identifier, statement)
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
 * The root SqlDriver creates 2 connections to the underlying database. One is used by
 * transactions and aligned with the thread performing the transaction. Multiple threads starting
 * transactions block and wait. The other connection does everything outside of a connection. The
 * goal is to be able to read while also writing. Future versions may create multiple query
 * connections.
 *
 * When a transaction is started, that thread is aligned with the transaction connection. Attempting
 * to start a transaction on another thread will block until the first finishes. Not ending
 * transactions is problematic, but it would be regardless.
 *
 * One implication to be aware of. You cannot operate on a single transaction from multiple threads.
 * However, it would be difficult to find a use case where this would be desirable or safe.
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
  private val maxConcurrentReader: Int = 1
) : ConnectionWrapper(), SqlDriver {
  constructor(
    configuration: DatabaseConfiguration,
    maxConcurrentReader: Int = 1
  ) : this(
    databaseManager = createDatabaseManager(configuration),
    maxConcurrentReader = maxConcurrentReader
  )

  constructor(
    schema: SqlDriver.Schema,
    name: String,
    maxConcurrentReader: Int = 1
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
    maxConcurrentReader = maxConcurrentReader
  )

  // A pool of reader connections used by all operations not in a transaction
  private val readerPool: Pool<ThreadConnection>

  // One and only writer connection which can be borrowed by any thread. Access to the connection is serialized.
  // In WAL mode (default), reads can happen through the reader pool, while this is also going on.
  private val writerPool: Pool<ThreadConnection>

  // Once a transaction is started and connection borrowed, it will be here, but only for that
  // thread
  private val borrowedConnectionThread = ThreadLocalRef<Borrowed<ThreadConnection>>()

  init {
    readerPool = Pool(maxConcurrentReader) {
      val connection = databaseManager.createMultiThreadedConnection()
      connection.withStatement("PRAGMA query_only = 1") { execute() }
      ThreadConnection(isReadOnly = true, connection, borrowedConnectionThread)
    }
    writerPool = Pool(1) {
      ThreadConnection(isReadOnly = false, databaseManager.createMultiThreadedConnection(), borrowedConnectionThread)
    }
  }

  override fun currentTransaction(): Transacter.Transaction? {
    return borrowedConnectionThread.get()?.value?.transaction?.value
  }

  override fun newTransaction(): Transacter.Transaction {
    val alreadyBorrowed = borrowedConnectionThread.get()
    return if (alreadyBorrowed == null) {
      val borrowed = writerPool.borrowEntry()

      try {
        val trans = borrowed.value.newTransaction()
        borrowedConnectionThread.value =
          borrowed.freeze() // Probably don't need to freeze, but revisit.
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

    if (mine != null) {
      // Reads can use any connection, while writes can only use the only writer connection in `writerPool`.
      if (readOnly || !mine.value.isReadOnly) {
        return mine.value.block()
      }

      throw IllegalStateException("Attempting to perform writes using a reader connection.")
    }

    return if (readOnly) {
      readerPool.access { it.block() }
    } else {
      writerPool.access { it.block() }
    }
  }

  override fun close() {
    writerPool.access { it.close() }
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
  val conn = SqliterWrappedConnection(ThreadConnection(isReadOnly = false, connection, null))
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
  val isReadOnly: Boolean,
  private val connection: DatabaseConnection,
  private val borrowedConnectionThread: ThreadLocalRef<Borrowed<ThreadConnection>>?
) : Closeable {

  internal val transaction: AtomicReference<Transacter.Transaction?> = AtomicReference(null)
  internal val closed:Boolean
    get() = connection.closed

  internal val statementCache = NativeCache<Statement>()

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
      transaction.value = enclosingTransaction

      if (enclosingTransaction == null) {
        try {
          if (successful) {
            connection.setTransactionSuccessful()
          }

          connection.endTransaction()
        } finally {
          // Release if we have
          borrowedConnectionThread?.let {
            it.get()?.release()
            it.value = null
          }
        }
      }
    }
  }
}
