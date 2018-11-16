package com.squareup.sqldelight.drivers.ios

import co.touchlab.sqliter.*
import co.touchlab.stately.collections.frozenHashMap
import co.touchlab.stately.collections.frozenLinkedList
import co.touchlab.stately.concurrency.AtomicBoolean
import co.touchlab.stately.concurrency.AtomicReference
import co.touchlab.stately.concurrency.QuickLock
import co.touchlab.stately.concurrency.withLock
import co.touchlab.stately.freeze
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.*
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.*

class SQLiterHelper(managerProducer: ()->DatabaseManager) : SqlDatabase, EnforceClosed by EnforceClosedImpl() {
    private val databaseManager: DatabaseManager = managerProducer()
    private val connection = SQLiterConnection(databaseManager.createConnection())

    override fun close() {
        checkNotClosed()
        trackClosed()
        connection.close()
    }

    override fun getConnection(): SqlDatabaseConnection {
        checkNotClosed()
        return connection
    }
}

/**
 * Wrap native database connection with SqlDatabaseConnection and
 * properly handle closing resources.
 */
fun wrapConnection(connection: DatabaseConnection, block:(SqlDatabaseConnection)->Unit){
    val conn = SQLiterConnection(connection)
    try {
        block(conn)
    }
    finally {
        conn.close(closeDbConnection = false)
    }
}

/**
 * For simplicity with the current API, the connection can be shared between threads. In theory, however,
 * in a "Saner Concurrency" context, we'd be better off if connections were tied to threads.
 *
 * SQLite cannot nest transactions, so there should be no situation where we have an enclosing transaction.
 * Transactions are isolated to connections, though, so if you're looking to have multiple transactions,
 * create multiple connections. This won't help a lot from a performance perspective because only one
 * connection can write at one time.
 */
internal class SQLiterConnection(
        internal val databaseConnection: DatabaseConnection
) : SqlDatabaseConnection, EnforceClosed by EnforceClosedImpl() {
    private val transaction: AtomicReference<Transaction?> = AtomicReference(null)
    private val transLock = QuickLock()
    private val statementList = frozenLinkedList<Statement>(stableIterator = false)
    private val queryList = frozenLinkedList<SQLiterQuery>(stableIterator = false)

    override fun currentTransaction(): Transacter.Transaction? = transaction.value

    override fun newTransaction(): Transacter.Transaction =
            transLock.withLock {
                val enclosing = transaction.value
                val trans = Transaction(enclosing).freeze()
                transaction.value = trans

                if(enclosing == null) {
                    databaseConnection.beginTransaction()
                }

                return trans
            }

    override fun prepareStatement(sql: String, type: SqlPreparedStatement.Type, parameters: Int): SqlPreparedStatement {
        checkNotClosed()

        return when(type) {
            SELECT -> {
                val query = SQLiterQuery(sql, databaseConnection)
                queryList.add(query)
                query
            }
            INSERT, UPDATE, DELETE, EXECUTE -> {
                val statment = databaseConnection.createStatement(sql)
                statementList.add(statment)
                SQLiterStatement(statment, type)
            }
        }
    }

    internal fun close(closeDbConnection:Boolean = true){
        checkNotClosed()
        trackClosed()
        statementList.forEach { it.finalizeStatement() }
        queryList.forEach { it.close() }
        if(closeDbConnection) {
            databaseConnection.close()
        }
    }

    inner class Transaction(
            override val enclosingTransaction: Transaction?
    ) : Transacter.Transaction() {

        override fun endTransaction(successful: Boolean) = transLock.withLock {
            if(enclosingTransaction == null) {
                if (successful) {
                    databaseConnection.setTransactionSuccessful()
                    databaseConnection.endTransaction()
                } else {
                    databaseConnection.endTransaction()
                }
            }
            transaction.value = enclosingTransaction
        }
    }
}

class SQLiterQuery(private val sql: String,
                   private val database: DatabaseConnection) :
        SqlPreparedStatement, EnforceClosed by EnforceClosedImpl() {
    internal val availableStatements = frozenLinkedList<Statement>(stableIterator = false)
    private val allStatements = frozenLinkedList<Statement>(stableIterator = false)

    private val queryLock = QuickLock()
    private val binds = frozenHashMap<Int, (Statement) -> Unit>()

    internal fun close(){
        queryLock.withLock {
            checkNotClosed()
            trackClosed()
            allStatements.forEach {
                it.finalizeStatement()
            }
        }
    }
    override fun bindBytes(index: Int, bytes: ByteArray?) {
        bytes.freeze()
        binds.put(index) { it.bindBlob(index, bytes) }
    }

    override fun bindDouble(index: Int, double: Double?) {
        binds.put(index) { it.bindDouble(index, double) }
    }

    override fun bindLong(index: Int, long: Long?) {
        binds.put(index) { it.bindLong(index, long) }
    }

    override fun bindString(index: Int, string: String?) {
        binds.put(index) { it.bindString(index, string) }
    }

    private fun bindTo(statement: Statement) {
        for (bind in binds.values.iterator()) {
            bind(statement)
        }
    }

    override fun execute() = throw UnsupportedOperationException()

    private fun findStatement():Statement = queryLock.withLock {
        checkNotClosed()
        return if (availableStatements.size == 0) {
            val statement = database.createStatement(sql)
            allStatements.add(statement)
            statement
        } else {
            availableStatements.removeAt(0)
        }
    }

    internal fun cacheStatement(statement: Statement){
        queryLock.withLock {
            checkNotClosed()
            statement.resetStatement()
            availableStatements.add(statement)
        }
    }

    override fun executeQuery(): SqlCursor {
        val stmt = findStatement()
        bindTo(stmt)
        return SQLiterCursor(stmt, this)
    }
}

class SQLiterStatement(private val statement: Statement, private val type: SqlPreparedStatement.Type) :
        SqlPreparedStatement {

    override fun bindBytes(index: Int, bytes: ByteArray?) {
        bytes.freeze()
        statement.bindBlob(index, bytes)
    }

    override fun bindDouble(index: Int, double: Double?) {
        statement.bindDouble(index, double)
    }

    override fun bindLong(index: Int, long: Long?) {
        statement.bindLong(index, long)
    }

    override fun bindString(index: Int, string: String?) {
        statement.bindString(index, string)
    }

    override fun execute() {
        when (type) {
            SELECT -> throw AssertionError()
            else -> statement.execute()
        }
    }

    override fun executeQuery(): SqlCursor = throw UnsupportedOperationException()
}

class SQLiterCursor(val statement: Statement, private val query:SQLiterQuery) : SqlCursor {
    private val cursor = statement.query()

    override fun close() {
        query.cacheStatement(statement)
    }

    override fun getBytes(index: Int): ByteArray? = cursor.getBytesOrNull(index)

    override fun getDouble(index: Int): Double? = cursor.getDoubleOrNull(index)

    override fun getLong(index: Int): Long? = cursor.getLongOrNull(index)

    override fun getString(index: Int): String? = cursor.getStringOrNull(index)

    override fun next(): Boolean = cursor.next()
}

interface EnforceClosed{
    fun trackClosed()
    fun checkNotClosed()
}
internal class EnforceClosedImpl: EnforceClosed{
    private val closed = AtomicBoolean(false)
    override fun trackClosed() {
        closed.value = true
    }

    override fun checkNotClosed() {
        if(closed.value)
            throw IllegalStateException("Closed")
    }

}