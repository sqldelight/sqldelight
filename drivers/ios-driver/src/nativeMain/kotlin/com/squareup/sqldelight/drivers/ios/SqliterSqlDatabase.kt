package com.squareup.sqldelight.drivers.ios

import co.touchlab.sqliter.*
import co.touchlab.stately.collections.*
import co.touchlab.stately.concurrency.*
import co.touchlab.stately.freeze
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlDatabaseConnection
import com.squareup.sqldelight.db.SqlPreparedStatement

/**
 * Driver implementation for Sqliter. The root SqlDatabase creates and manages a pool of actual connections to the
 * underlying database. Operations performed outside of a transaction are performed on a shared connection. These
 * would generally include queries or small inserts/updates.
 *
 * When a transaction is started, that thread is aligned with a real connection, which is no longer shared until
 * that transaction completes. Currently not finishing transactions means accumulating connections, so users should
 * be careful to close transactions (but they should be doing this anyway).
 *
 * To use Sqldelight during create/upgrade processes, you can alternatively wrap a real connection with wrapConnection.
 *
 * SqlPreparedStatement instances also do not point to real resources until either execute or executeQuery is called.
 * The SqlPreparedStatement structure also maintains a thread-aligned instance which accumulates bind calls. Those are
 * replayed on a real sqlite statement instance when execute or executeQuery is called. This avoids race conditions
 * with bind calls.
 */
internal class SqliterSqlDatabase(private val databaseManager: DatabaseManager) : SqlDatabase, RealDatabaseContext {

    internal val connectionCache = ThreadLocalCache {
        ThreadConnection(databaseManager.createMultiThreadedConnection(), this)
    }

    private val connectionLock = Lock()
    private val singleOpConnection = ThreadConnection(databaseManager.createMultiThreadedConnection(), this)
    private val publicApiConnection = SqliterSqlDatabaseConnection(this)

    override fun close() = connectionLock.withLock {
        connectionCache.clear { it.connection.close() }
        singleOpConnection.close()
    }

    override fun getConnection(): SqlDatabaseConnection = publicApiConnection

    /**
     * If we're in a transaction, then I have a connection. Otherwise we lock and
     * use the open connection on which all other ops run.
     */
    override fun <R> accessConnection(block: ThreadConnection.() -> R): R{
        val mine = connectionCache.mineOrNone()
        return if (mine != null)
        {
            mine.block()
        }
        else {
            connectionLock.withLock {
                singleOpConnection.block()
            }
        }
    }
}

/**
 * External call to create driver instance. Used to keep internal class definitions, but can be modified.
 */
fun createSqlDatabase(databaseManager: DatabaseManager):SqlDatabase = SqliterSqlDatabase(databaseManager)

/**
 * Sqliter's DatabaseConfiguration takes lambda arguments for it's create and upgrade operations, which each take a DatabaseConnection
 * argument. Use wrapConnection to have Sqldelight access this passed connection and avoid the pooling that the full
 * SqlDatabase instance performs.
 *
 * Note that queries created during this operation will be cleaned up. If holding onto a cursor from a wrap call, it will
 * no longer be viable.
 */
fun wrapConnection(
        connection: DatabaseConnection,
        block: (SqlDatabaseConnection) -> Unit
) {
    val conn = SqliterWrappedConnection(ThreadConnection(connection, null))
    try {
        block(conn)
    } finally {
        conn.close()
    }
}

/**
 * SqlDatabaseConnection that wraps a Sqliter connection. Useful for migration tasks, or if you don't want the polling.
 */
internal class SqliterWrappedConnection(private val threadConnection: ThreadConnection):SqlDatabaseConnection, RealDatabaseContext{
    override fun currentTransaction(): Transacter.Transaction? = threadConnection.transaction.value

    override fun newTransaction(): Transacter.Transaction = threadConnection.newTransaction()

    override fun prepareStatement(sql: String, type: SqlPreparedStatement.Type, parameters: Int): SqlPreparedStatement =
            SqliterSqlPreparedStatement(sql, type, this)

    override fun <R> accessConnection(block: ThreadConnection.() -> R): R = threadConnection.block()

    fun close() {
        threadConnection.cleanUp()
    }
}

/**
 * Implementation of SqlDatabaseConnection. This does not actually have a single database connection. Calling
 * newTransaction will trigger SqlighterSqlDatabase's connection thread alignment, but otherwise, this class is
 * mostly just passing calls onto other classes.
 *
 * prepareStatement returns 'SqlPreparedStatement', which in a similar way does not resolve to an actual sqlite
 * resource until an attempt to execute it happens.
 */
internal class SqliterSqlDatabaseConnection(private val database: SqliterSqlDatabase) : SqlDatabaseConnection {
    override fun currentTransaction(): Transacter.Transaction? = database.connectionCache.mineOrNone()?.transaction?.value

    override fun newTransaction(): Transacter.Transaction {
        val myConn = database.connectionCache.mineOrAlign()
        return myConn.newTransaction()
    }

    override fun prepareStatement(sql: String, type: SqlPreparedStatement.Type, parameters: Int): SqlPreparedStatement =
            SqliterSqlPreparedStatement(sql, type, database)
}

/**
 * Wraps and manages a "real" database connection. In a pooled scenario, this may be the shared global connection or
 * one of the thread aligned ones. In a wrapped scenario, it's simply "the" connection. sqlLighterDatabase is provided
 * as a hook to allow removing the thread alignment when transactions finish.
 *
 * Sqlite statements are specific to connections, and must be finalized explicitly. Cursors are backed by a statement resource,
 * so we keep links to open cursors to allow us to close them out properly in cases where the user does not.
 */
internal class ThreadConnection(val connection: DatabaseConnection, private val sqlLighterDatabase: SqliterSqlDatabase?) {
    internal val transaction: AtomicReference<Transaction?> = AtomicReference(null)
    /**
     * Keep all outstanding cursors to close when closing the db, just in case the user didn't.
     */
    internal val cursorCollection = frozenLinkedList<Cursor>() as SharedLinkedList<Cursor>

    internal val statementCache = frozenHashMap<String, Statement>() as SharedHashMap<String, Statement>

    fun <R> withStatement(sql:String, block:Statement.()->R):R{
        val statement = findCreateStatement(sql)
        return statement.block()
    }

    private fun findCreateStatement(sql:String):Statement{
        val statement = statementCache.get(sql)
        if(statement != null){
            return statement
        }

        val newStatement = connection.createStatement(sql)
        statementCache.put(sql, newStatement)
        return newStatement
    }

    fun removeCreateStatement(sql:String):Statement{
        val cached = statementCache.remove(sql)
        if(cached != null)
            return cached

        return connection.createStatement(sql)
    }

    fun newTransaction(): Transaction {
        val enclosing = transaction.value

        //Create here, in case we bomb...
        if (enclosing == null) {
            connection.beginTransaction()
        }

        val trans = Transaction(enclosing).freeze()
        transaction.value = trans

        return trans
    }

    inner class Transaction(override val enclosingTransaction: Transaction?) : Transacter.Transaction() {

        override fun endTransaction(successful: Boolean) {
            if (enclosingTransaction == null) {
                try {
                    if (successful) {
                        connection.setTransactionSuccessful()
                    }

                    connection.endTransaction()
                } finally {
                    sqlLighterDatabase?.connectionCache?.mineRelease()
                }

            }
            transaction.value = enclosingTransaction
        }
    }

    internal fun trackCursor(cursor: Cursor, sql: String):Recycler = CursorRecycler(cursorCollection.addNode(cursor), sql)

    internal fun cleanUp(){
        cursorCollection.cleanUp {
            it.statement.finalizeStatement()
        }
        statementCache.cleanUp {
            it.value.finalizeStatement()
        }
    }

    internal fun close(){
        cleanUp()
        connection.close()
    }

    private inner class CursorRecycler(private val node: AbstractSharedLinkedList.Node<Cursor>, private val sql:String):Recycler{
        override fun recycle() {
            node.remove()
            val statement = node.nodeValue.statement
            statement.resetStatement()
            val removed = statementCache.put(sql, statement)
            removed?.let {
                it.finalizeStatement()
            }
        }
    }
}

/**
 * This belongs in Stately and will be moved there soon.
 */
internal fun <T> SharedLinkedList<T>.cleanUp(block:(T)->Unit){
    val extractList = kotlin.collections.ArrayList<T>(size)
    extractList.addAll(this)
    this.clear()
    extractList.forEach(block)
}

internal fun <K, V> SharedHashMap<K, V>.cleanUp(block:(Map.Entry<K, V>)->Unit){
    val extractMap = kotlin.collections.HashMap<K, V>(this.size)
    extractMap.putAll(this)
    this.clear()
    extractMap.forEach(block)
}

internal interface RealDatabaseContext{
    //Only one thread can access each connection at a time
    fun <R> accessConnection(block: ThreadConnection.() -> R):R
}

/**
 * Simple hook for recycling cursors
 */
internal interface Recycler{
    fun recycle()
}

/**
 * Shared interface for a particular sql statement. To prevent issues with multiple threads using the same instance,
 * this class maintains a thread cache of binding collectors, which collect binding operations called on them, and
 * replay them on "real" database statements.
 */
internal class SqliterSqlPreparedStatement(
        private val sql: String,
        private val type: SqlPreparedStatement.Type,
        private val realDatabaseContext: RealDatabaseContext
) : SqlPreparedStatement {
    private val cacheStrategy = if(type == SqlPreparedStatement.Type.SELECT){SelectStatements()}else{MutatorStatements()}

    override fun bindBytes(index: Int, value: ByteArray?) = cacheStrategy.myStatementInstance().bindBytes(index, value)
    override fun bindDouble(index: Int, value: Double?) = cacheStrategy.myStatementInstance().bindDouble(index, value)
    override fun bindLong(index: Int, value: Long?) = cacheStrategy.myStatementInstance().bindLong(index, value)
    override fun bindString(index: Int, value: String?) = cacheStrategy.myStatementInstance().bindString(index, value)

    /**
     * Executing a statement clears the instance definition. Effectively that means the bindings are reset. We can
     * recycle these rather than letting them be finalized in sqlite, which should improve performance, but we're
     * working on "simple" right now.
     */
    override fun execute() {
        realDatabaseContext.accessConnection {
            withStatement(sql) {
                applyBindings(this)
                when (type) {
                    SqlPreparedStatement.Type.SELECT -> throw kotlin.AssertionError()
                    else -> execute()
                }
            }
        }
        removeMyInstance()
    }

    /**
     * Creating a cursor returns an actual sqlite statement instance, so we need to be careful with these.
     *
     * The bindings for a query and an execute seem to work a little differently.
     */
    override fun executeQuery(): SqlCursor = realDatabaseContext.accessConnection {
        val statement = removeCreateStatement(sql)
        applyBindings(statement)
        val cursor = statement.query()
        SqliterSqlCursor(cursor, trackCursor(cursor, sql))
    }

    private fun applyBindings(statement: Statement){
        cacheStrategy.myStatementInstance().binds.forEach { it.value(statement) }
    }

    private fun removeMyInstance() {
        cacheStrategy.releaseInstance()
    }
}

/**
 * Record binding value to be played back later on "real" database artifacts.
 */
internal class BindingAccumulator {
    internal val binds = frozenHashMap<Int, (Statement) -> Unit>()

    fun bindBytes(index: Int, value: ByteArray?) {
        binds.put(index) { it.bindBlob(index, value) }
    }

    fun bindDouble(index: Int, value: Double?) {
        binds.put(index) { it.bindDouble(index, value) }
    }

    fun bindLong(index: Int, value: Long?) {
        binds.put(index) { it.bindLong(index, value) }
    }

    fun bindString(index: Int, value: String?) {
        binds.put(index) { it.bindString(index, value) }
    }
}

/**
 * Select statements are created with binding params are aren't cleared out. Different threads use the same ones,
 * and they aren't cleared or (as far as I can tell) modified. Mutating statements, OTOH, are re-bound each time, and
 * we need to worry about multiple threads using the same statement. This interface reflects those two realities and
 * attempts to hide the details.
 */
internal interface StatementCacheStrategy{
    fun myStatementInstance():BindingAccumulator
    fun releaseInstance()
}

/**
 * Each thread may get a different instance of the underlying binding accumulator. The end result being multiple threads
 * can bind to the same "statement" without stepping on each other. Executing the statement dissociates it from the current
 * thread, so as long as calling clients that start binding also execute, you shouldn't have leaks.
 */
internal class MutatorStatements():StatementCacheStrategy{
    private val statementCache = ThreadLocalCache {BindingAccumulator()}
    override fun myStatementInstance(): BindingAccumulator = statementCache.mineOrAlign()
    override fun releaseInstance() = statementCache.mineRelease()
}

/**
 * Binding parameters for queries are passed in when creating the statement, shouldn't be cleared out, and aren't modified.
 * We don't need a pool of them, and they should just kind of hang out.
 */
internal class SelectStatements():StatementCacheStrategy{
    override fun myStatementInstance(): BindingAccumulator = statement

    override fun releaseInstance() {
        //¯\_(ツ)_/¯
    }

    private val statement = BindingAccumulator()
}

/**
 * Wrapper for cursor calls. Cursors point to real sqlite statements, so we need to be careful with them. If dev
 * closes the outer structure, this will get closed as well, which means it could start throwing errors if you're trying
 * to access it.
 */
internal class SqliterSqlCursor(private val cursor: Cursor, private val recycler: Recycler) : SqlCursor {
    override fun close() {
        recycler.recycle()
    }

    override fun getBytes(index: Int): ByteArray? = cursor.getBytesOrNull(index)

    override fun getDouble(index: Int): Double? = cursor.getDoubleOrNull(index)

    override fun getLong(index: Int): Long? = cursor.getLongOrNull(index)

    override fun getString(index: Int): String? = cursor.getStringOrNull(index)

    override fun next(): Boolean = cursor.next()
}

/**
 * Thread local cache that allows one instance to be associated with a thread, but retains global instance access.
 * Instances are cached and reused as needed. The caller needs to release the association when done. There is no cap
 * on cache size or policy to reassign entries that are currently in use, so make sure you're using this appropriately.
 */
internal class ThreadLocalCache<T>(private val producer:()->T){
    private val cache = frozenLinkedList<CacheEntry<T>>()
    private val localRef = ThreadLocalRef<CacheEntry<T>>()
    private val cacheLock = Lock()

    fun mineOrNone():T? = localRef.value?.entry

    fun mineOrAlign():T{
        val mine = localRef.value
        if(mine != null)
            return mine.entry

        return cacheLock.withLock {
            val unaligned = cache.find { !it.inUse.value } ?: createEntry()
            localRef.value = unaligned
            unaligned.inUse.value = true
            unaligned.entry
        }
    }

    fun mineRelease() = cacheLock.withLock {
        val myEntry = localRef.value
        if(myEntry != null){
            localRef.value = null
            myEntry.inUse.value = false
        }
    }

    private fun createEntry():CacheEntry<T>{
        val newVal = producer().freeze()
        val entry = CacheEntry(newVal)
        cache.add(entry)
        return entry
    }

    fun clear(clearBlock:(T)->Unit = {}) = cacheLock.withLock {
        cache.forEach {
            clearBlock(it.entry)
        }
        cache.clear()
    }

    class CacheEntry<T>(val entry:T){
        val inUse = AtomicBoolean(false)
    }
}

