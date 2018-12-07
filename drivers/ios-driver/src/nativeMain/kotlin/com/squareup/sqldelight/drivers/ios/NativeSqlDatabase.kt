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
 * Native driver implementation.
 *
 * The root SqlDatabase creates 2 connections to the underlying database. One is used by transactions and aligned
 * with the thread performing the transaction. Multiple threads starting transactions block and wait. The other
 * connection does everything outside of a connection. The goal is to be able to read while also writing. Future
 * versions may create multiple query connections.
 *
 * When a transaction is started, that thread is aligned with the transaction connection. Attempting to start a
 * transaction on another thread will block until the first finishes. Not ending transactions is problematic, but it
 * would be regardless.
 *
 * One implication to be aware of. You cannot operate on a single transaction from multiple threads. However, it would
 * be difficult to find a use case where this would be desirable or safe.
 *
 * To use Sqldelight during create/upgrade processes, you can alternatively wrap a real connection with wrapConnection.
 *
 * SqlPreparedStatement instances also do not point to real resources until either execute or executeQuery is called.
 * The SqlPreparedStatement structure also maintains a thread-aligned instance which accumulates bind calls. Those are
 * replayed on a real sqlite statement instance when execute or executeQuery is called. This avoids race conditions
 * with bind calls.
 */
class NativeSqlDatabase(private val databaseManager: DatabaseManager) : SqlDatabase, RealDatabaseContext {

    constructor(
            configuration: DatabaseConfiguration
    ) : this(
            databaseManager = createDatabaseManager(configuration)
    )

    constructor(
            schema: SqlDatabase.Schema,
            name: String
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
            )
    )

    //Connection used by all operations not in a transaction
    internal val queryPool = SinglePool {
        ThreadConnection(databaseManager.createMultiThreadedConnection(), this)
    }

    //Connection which can be borrowed by a thread, to ensure all transaction ops happen in the same place
    //In WAL mode (default) reads can happen while this is also going on
    internal val transactionPool = SinglePool {
        ThreadConnection(databaseManager.createMultiThreadedConnection(), this)
    }

    //Once a transaction is started and connection borrowed, it will be here, but only for that thread
    internal val borrowedConnectionThread = ThreadLocalRef<SinglePool<ThreadConnection>.Borrowed<ThreadConnection>>()

    internal val publicApiConnection = NativeSqlDatabaseConnection(this)

    override fun close() {
        transactionPool.access { it.close() }
        queryPool.access { it.close() }
    }

    override fun getConnection(): SqlDatabaseConnection = publicApiConnection

    /**
     * If we're in a transaction, then I have a connection. Otherwise use shared.
     */
    override fun <R> accessConnection(select: Boolean, block: ThreadConnection.() -> R): R{
        val mine = borrowedConnectionThread.get()
        return if (mine != null)
        {
            mine.entry.block()
        }
        else {
            queryPool.access { it.block() }
        }
    }
}

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
            NativeSqlPreparedStatement(sql, type, this)

    override fun <R> accessConnection(select: Boolean, block: ThreadConnection.() -> R): R = threadConnection.block()

    fun close() {
        threadConnection.cleanUp()
    }
}

/**
 * Implementation of SqlDatabaseConnection. This does not actually have a db connection. It delegates to NativeSqlDatabase.
 */
internal class NativeSqlDatabaseConnection(private val database: NativeSqlDatabase) : SqlDatabaseConnection {
    override fun currentTransaction(): Transacter.Transaction? = database.borrowedConnectionThread.get()?.entry?.transaction?.value

    override fun newTransaction(): Transacter.Transaction {
        val alreadyBorrowed = database.borrowedConnectionThread.get()
        return if(alreadyBorrowed == null) {
            val borrowed = database.transactionPool.borrowEntry()
            try {
                val trans = borrowed.entry.newTransaction()
                database.borrowedConnectionThread.value = borrowed.freeze() //Probably don't need to freeze, but revisit
                trans
            } catch (e: Throwable) {
                //Unlock on failure
                borrowed.release()
                throw e
            }
        }else{
            alreadyBorrowed.entry.newTransaction()
        }
    }

    override fun prepareStatement(sql: String, type: SqlPreparedStatement.Type, parameters: Int): SqlPreparedStatement =
            NativeSqlPreparedStatement(sql, type, database)
}

/**
 * Wraps and manages a "real" database connection.
 *
 * Sqlite statements are specific to connections, and must be finalized explicitly. Cursors are backed by a statement resource,
 * so we keep links to open cursors to allow us to close them out properly in cases where the user does not.
 *
 */
class ThreadConnection(val connection: DatabaseConnection, private val sqlLighterDatabase: NativeSqlDatabase?) {
    internal val transaction: AtomicReference<Transaction?> = AtomicReference(null)

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
        safePut(sql, newStatement)
        return newStatement
    }

    private fun safePut(sql:String, statement: Statement)
    {
        val removed = statementCache.put(sql, statement)
        removed?.let {
            it.finalizeStatement()
        }
    }

    /**
     * For cursors. Cursors are actually backed by sqlite statement instances, so they need to be removed
     * from the cache when in use. We're giving out a sqlite resource here, so extra care.
     */
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
            //This stays here to avoid a race condition with multiple threads and transactions
            transaction.value = enclosingTransaction

            if (enclosingTransaction == null) {
                try {
                    if (successful) {
                        connection.setTransactionSuccessful()
                    }

                    connection.endTransaction()
                } finally {
                    //Release if we have
                    sqlLighterDatabase?.let {
                        it.borrowedConnectionThread?.get()?.release()
                        it.borrowedConnectionThread.value = null
                    }
                }
            }
        }
    }

    internal fun trackCursor(cursor: Cursor, sql: String):Recycler = CursorRecycler(cursorCollection.addNode(cursor), sql)

    /**
     * This should only be called directly from wrapConnection. Clean resources without actually closing
     * the underlying connection.
     */
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
            safePut(sql, statement)
        }
    }
}

/**
 * This should probably be in Stately
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
    fun <R> accessConnection(select:Boolean, block: ThreadConnection.() -> R):R
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
internal class NativeSqlPreparedStatement(
        private val sql: String,
        private val type: SqlPreparedStatement.Type,
        private val realDatabaseContext: RealDatabaseContext
) : SqlPreparedStatement {
    private val cacheStrategy = if(type == SqlPreparedStatement.Type.SELECT){SelectStatements()}else{MutatorStatements()}

    override fun bindBytes(index: Int, value: ByteArray?) = cacheStrategy.myStatementInstance().bindBytes(index, value)
    override fun bindDouble(index: Int, value: Double?) = cacheStrategy.myStatementInstance().bindDouble(index, value)
    override fun bindLong(index: Int, value: Long?) = cacheStrategy.myStatementInstance().bindLong(index, value)
    override fun bindString(index: Int, value: String?) = cacheStrategy.myStatementInstance().bindString(index, value)

    override fun execute() {
        realDatabaseContext.accessConnection(false) {
            withStatement(sql) {
                applyReleaseBindings(this)
                when (type) {
                    SqlPreparedStatement.Type.SELECT -> throw kotlin.AssertionError()
                    else -> execute()
                }
            }
        }
    }

    /**
     * Creating a cursor returns an actual sqlite statement instance, so we need to be careful with these.
     */
    override fun executeQuery(): SqlCursor = realDatabaseContext.accessConnection(true) {
        val statement = removeCreateStatement(sql)
        try {
            applyReleaseBindings(statement)
            val cursor = statement.query()
            SqliterSqlCursor(cursor, trackCursor(cursor, sql))
        } catch (e: Exception) {
            statement.finalizeStatement()
            throw e
        }
    }

    private fun applyReleaseBindings(statement: Statement){
        try {
            cacheStrategy.myStatementInstance().binds.forEach { it.value(statement) }
        } finally {
            cacheStrategy.releaseInstance()
        }
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
 * we need to worry about multiple threads using the same statement. This interface reflects those two realities.
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
    internal val statementCache = ThreadLocalCache {BindingAccumulator()}
    override fun myStatementInstance(): BindingAccumulator = statementCache.mineOrAlign()
    override fun releaseInstance() {
        statementCache.mineOrNone()?.let {
            it.binds.clear()
        }
        statementCache.mineRelease()
    }
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
 * Simple single entry "pool". Sufficient for the vast majority of sqlite needs, but will need a more exotic structure
 * for an actual pool.
 */
internal class SinglePool<T>(producer:()->T){
    private val lock = Lock()
    internal val entry = producer()
    private val borrowed = AtomicBoolean(false)

    fun <R> access(block:(T)->R):R = lock.withLock {
        block(entry)
    }

    fun borrowEntry():Borrowed<T> {
        lock.lock()
        assert(!borrowed.value)
        borrowed.value = true
        return Borrowed(entry)
    }

    inner class Borrowed<T>(val entry:T){
        fun release(){
            borrowed.value = false
            lock.unlock()
        }
    }
}

/**
 * Thread local cache that allows one instance to be associated with a thread, but retains global instance access.
 * Instances are cached and reused as needed. The caller needs to release the association when done. There is no cap
 * on cache size or policy to reassign entries that are currently in use, so make sure you're using this appropriately.
 */
internal class ThreadLocalCache<T>(private val producer:()->T){
    internal val cache = frozenLinkedList<CacheEntry<T>>()
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
            if(it.inUse.value)
                throw IllegalStateException("Cache entry in use. Cannot clear.")
        }
        cache.forEach {
            clearBlock(it.entry)
        }
        cache.clear()
    }

    class CacheEntry<T>(val entry:T){
        val inUse = AtomicBoolean(false)
    }
}

