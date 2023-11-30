package app.cash.sqldelight.driver.android

import android.content.Context
import android.database.AbstractWindowedCursor
import android.database.Cursor
import android.database.CursorWindow
import android.os.Build
import android.util.LruCache
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteProgram
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.Api28Impl.setWindowSize

private const val DEFAULT_CACHE_SIZE = 20

class AndroidSqliteDriver private constructor(
  private val openHelper: SupportSQLiteOpenHelper? = null,
  database: SupportSQLiteDatabase? = null,
  private val cacheSize: Int,
  private val windowSizeBytes: Long? = null,
) : SqlDriver {
  init {
    require((openHelper != null) xor (database != null))
  }

  private val transactions = ThreadLocal<Transacter.Transaction>()
  private val database by lazy {
    openHelper?.writableDatabase ?: database!!
  }

  constructor(
    openHelper: SupportSQLiteOpenHelper,
  ) : this(openHelper = openHelper, database = null, cacheSize = DEFAULT_CACHE_SIZE, windowSizeBytes = null)

  /**
   * @param [cacheSize] The number of compiled sqlite statements to keep in memory per connection.
   *   Defaults to 20.
   * @param [useNoBackupDirectory] Sets whether to use a no backup directory or not.
   * @param [windowSizeBytes] Size of cursor window in bytes, per [CursorWindow] (Android 28+ only), or null to use the default.
   */
  @JvmOverloads
  constructor(
    schema: SqlSchema<QueryResult.Value<Unit>>,
    context: Context,
    name: String? = null,
    factory: SupportSQLiteOpenHelper.Factory = FrameworkSQLiteOpenHelperFactory(),
    callback: SupportSQLiteOpenHelper.Callback = AndroidSqliteDriver.Callback(schema),
    cacheSize: Int = DEFAULT_CACHE_SIZE,
    useNoBackupDirectory: Boolean = false,
    windowSizeBytes: Long? = null,
  ) : this(
    database = null,
    openHelper = factory.create(
      SupportSQLiteOpenHelper.Configuration.builder(context)
        .callback(callback)
        .name(name)
        .noBackupDirectory(useNoBackupDirectory)
        .build(),
    ),
    cacheSize = cacheSize,
    windowSizeBytes = windowSizeBytes,
  )

  @JvmOverloads
  constructor(
    database: SupportSQLiteDatabase,
    cacheSize: Int = DEFAULT_CACHE_SIZE,
    windowSizeBytes: Long? = null,
  ) : this(openHelper = null, database = database, cacheSize = cacheSize, windowSizeBytes = windowSizeBytes)

  private val statements = object : LruCache<Int, AndroidStatement>(cacheSize) {
    override fun entryRemoved(
      evicted: Boolean,
      key: Int,
      oldValue: AndroidStatement,
      newValue: AndroidStatement?,
    ) {
      if (evicted) oldValue.close()
    }
  }

  private val listeners = linkedMapOf<String, MutableSet<Query.Listener>>()

  override fun addListener(vararg queryKeys: String, listener: Query.Listener) {
    synchronized(listeners) {
      queryKeys.forEach {
        listeners.getOrPut(it, { linkedSetOf() }).add(listener)
      }
    }
  }

  override fun removeListener(vararg queryKeys: String, listener: Query.Listener) {
    synchronized(listeners) {
      queryKeys.forEach {
        listeners[it]?.remove(listener)
      }
    }
  }

  override fun notifyListeners(vararg queryKeys: String) {
    val listenersToNotify = linkedSetOf<Query.Listener>()
    synchronized(listeners) {
      queryKeys.forEach { listeners[it]?.let(listenersToNotify::addAll) }
    }
    listenersToNotify.forEach(Query.Listener::queryResultsChanged)
  }

  override fun newTransaction(): QueryResult<Transacter.Transaction> {
    val enclosing = transactions.get()
    val transaction = Transaction(enclosing)
    transactions.set(transaction)

    if (enclosing == null) {
      database.beginTransactionNonExclusive()
    }

    return QueryResult.Value(transaction)
  }

  override fun currentTransaction(): Transacter.Transaction? = transactions.get()

  inner class Transaction(
    override val enclosingTransaction: Transacter.Transaction?,
  ) : Transacter.Transaction() {
    override fun endTransaction(successful: Boolean): QueryResult<Unit> {
      if (enclosingTransaction == null) {
        if (successful) {
          database.setTransactionSuccessful()
          database.endTransaction()
        } else {
          database.endTransaction()
        }
      }
      transactions.set(enclosingTransaction)
      return QueryResult.Unit
    }
  }

  private fun <T> execute(
    identifier: Int?,
    createStatement: () -> AndroidStatement,
    binders: (SqlPreparedStatement.() -> Unit)?,
    result: AndroidStatement.() -> T,
  ): QueryResult.Value<T> {
    var statement: AndroidStatement? = null
    if (identifier != null) {
      statement = statements.remove(identifier)
    }
    if (statement == null) {
      statement = createStatement()
    }
    try {
      if (binders != null) {
        statement.binders()
      }
      return QueryResult.Value(statement.result())
    } finally {
      if (identifier != null) {
        statements.put(identifier, statement)?.close()
      } else {
        statement.close()
      }
    }
  }

  override fun execute(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?,
  ): QueryResult<Long> = execute(identifier, { AndroidPreparedStatement(database.compileStatement(sql)) }, binders, { execute() })

  override fun <R> executeQuery(
    identifier: Int?,
    sql: String,
    mapper: (SqlCursor) -> QueryResult<R>,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?,
  ) = execute(identifier, { AndroidQuery(sql, database, parameters, windowSizeBytes) }, binders) { executeQuery(mapper) }

  override fun close() {
    statements.evictAll()
    return openHelper?.close() ?: database.close()
  }

  open class Callback(
    private val schema: SqlSchema<QueryResult.Value<Unit>>,
    private vararg val callbacks: AfterVersion,
  ) : SupportSQLiteOpenHelper.Callback(
    if (schema.version > Int.MAX_VALUE) error("Schema version is larger than Int.MAX_VALUE: ${schema.version}.") else schema.version.toInt(),
  ) {

    override fun onCreate(db: SupportSQLiteDatabase) {
      schema.create(AndroidSqliteDriver(openHelper = null, database = db, cacheSize = 1))
    }

    override fun onUpgrade(
      db: SupportSQLiteDatabase,
      oldVersion: Int,
      newVersion: Int,
    ) {
      schema.migrate(
        AndroidSqliteDriver(openHelper = null, database = db, cacheSize = 1),
        oldVersion.toLong(),
        newVersion.toLong(),
        *callbacks,
      )
    }
  }
}

internal interface AndroidStatement : SqlPreparedStatement {
  fun execute(): Long
  fun <R> executeQuery(mapper: (SqlCursor) -> QueryResult<R>): R
  fun close()
}

private class AndroidPreparedStatement(
  private val statement: SupportSQLiteStatement,
) : AndroidStatement {
  override fun bindBytes(index: Int, bytes: ByteArray?) {
    if (bytes == null) statement.bindNull(index + 1) else statement.bindBlob(index + 1, bytes)
  }

  override fun bindLong(index: Int, long: Long?) {
    if (long == null) statement.bindNull(index + 1) else statement.bindLong(index + 1, long)
  }

  override fun bindDouble(index: Int, double: Double?) {
    if (double == null) statement.bindNull(index + 1) else statement.bindDouble(index + 1, double)
  }

  override fun bindString(index: Int, string: String?) {
    if (string == null) statement.bindNull(index + 1) else statement.bindString(index + 1, string)
  }

  override fun bindBoolean(index: Int, boolean: Boolean?) {
    if (boolean == null) {
      statement.bindNull(index + 1)
    } else {
      statement.bindLong(index + 1, if (boolean) 1L else 0L)
    }
  }

  override fun <R> executeQuery(mapper: (SqlCursor) -> QueryResult<R>): R = throw UnsupportedOperationException()

  override fun execute(): Long {
    return statement.executeUpdateDelete().toLong()
  }

  override fun close() {
    statement.close()
  }
}

private class AndroidQuery(
  override val sql: String,
  private val database: SupportSQLiteDatabase,
  override val argCount: Int,
  private val windowSizeBytes: Long?,
) : SupportSQLiteQuery, AndroidStatement {
  private val binds = MutableList<((SupportSQLiteProgram) -> Unit)?>(argCount) { null }

  override fun bindBytes(index: Int, bytes: ByteArray?) {
    binds[index] = { if (bytes == null) it.bindNull(index + 1) else it.bindBlob(index + 1, bytes) }
  }

  override fun bindLong(index: Int, long: Long?) {
    binds[index] = { if (long == null) it.bindNull(index + 1) else it.bindLong(index + 1, long) }
  }

  override fun bindDouble(index: Int, double: Double?) {
    binds[index] = { if (double == null) it.bindNull(index + 1) else it.bindDouble(index + 1, double) }
  }

  override fun bindString(index: Int, string: String?) {
    binds[index] = { if (string == null) it.bindNull(index + 1) else it.bindString(index + 1, string) }
  }

  override fun bindBoolean(index: Int, boolean: Boolean?) {
    binds[index] = {
      if (boolean == null) {
        it.bindNull(index + 1)
      } else {
        it.bindLong(index + 1, if (boolean) 1L else 0L)
      }
    }
  }

  override fun execute() = throw UnsupportedOperationException()

  override fun <R> executeQuery(mapper: (SqlCursor) -> QueryResult<R>): R {
    return database.query(this)
      .use { cursor -> mapper(AndroidCursor(cursor, windowSizeBytes)).value }
  }

  override fun bindTo(statement: SupportSQLiteProgram) {
    for (action in binds) {
      action!!(statement)
    }
  }

  override fun toString() = sql

  override fun close() {}
}

private class AndroidCursor(
  private val cursor: Cursor,
  windowSizeBytes: Long?,
) : SqlCursor {
  init {
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
      windowSizeBytes != null &&
      cursor is AbstractWindowedCursor
    ) {
      cursor.setWindowSize(windowSizeBytes)
    }
  }

  override fun next(): QueryResult.Value<Boolean> = QueryResult.Value(cursor.moveToNext())
  override fun getString(index: Int) = if (cursor.isNull(index)) null else cursor.getString(index)
  override fun getLong(index: Int) = if (cursor.isNull(index)) null else cursor.getLong(index)
  override fun getBytes(index: Int) = if (cursor.isNull(index)) null else cursor.getBlob(index)
  override fun getDouble(index: Int) = if (cursor.isNull(index)) null else cursor.getDouble(index)
  override fun getBoolean(index: Int) = if (cursor.isNull(index)) null else cursor.getLong(index) == 1L
}

@RequiresApi(Build.VERSION_CODES.P)
private object Api28Impl {
  @JvmStatic
  @DoNotInline
  fun AbstractWindowedCursor.setWindowSize(windowSizeBytes: Long) {
    window = CursorWindow(null, windowSizeBytes)
  }
}
