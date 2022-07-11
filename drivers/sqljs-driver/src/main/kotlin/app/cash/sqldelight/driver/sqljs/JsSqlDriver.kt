package app.cash.sqldelight.driver.sqljs

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.db.SqlSchema
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import kotlin.js.Promise

fun Promise<Database>.driver(): Promise<SqlDriver> = then { JsSqlDriver(it) }

fun Promise<SqlDriver>.withSchema(schema: SqlSchema? = null): Promise<SqlDriver> = then {
  schema?.create(it)
  it
}

fun Promise<SqlDriver>.transacter(): Promise<Transacter> = then { object : TransacterImpl(it) {} }

fun initSqlDriver(schema: SqlSchema? = null): Promise<SqlDriver> = initDb().driver().withSchema(schema)

class JsSqlDriver(private val db: Database) : SqlDriver {

  private val statements = mutableMapOf<Int, Statement>()
  private var transaction: Transacter.Transaction? = null
  private val listeners = mutableMapOf<String, MutableSet<Query.Listener>>()

  override fun addListener(listener: Query.Listener, queryKeys: Array<String>) {
    queryKeys.forEach {
      listeners.getOrPut(it, { mutableSetOf() }).add(listener)
    }
  }

  override fun removeListener(listener: Query.Listener, queryKeys: Array<String>) {
    queryKeys.forEach {
      listeners[it]?.remove(listener)
    }
  }

  override fun notifyListeners(queryKeys: Array<String>) {
    queryKeys.flatMap { listeners[it].orEmpty() }
      .distinct()
      .forEach(Query.Listener::queryResultsChanged)
  }

  override fun <R> executeQuery(
    identifier: Int?,
    sql: String,
    mapper: (SqlCursor) -> R,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?,
  ): QueryResult<R> {
    val cursor = createOrGetStatement(identifier, sql).run {
      bind(parameters, binders)
      JsSqlCursor(this)
    }

    return try {
      QueryResult.Value(mapper(cursor))
    } finally {
      cursor.close()
    }
  }

  override fun execute(identifier: Int?, sql: String, parameters: Int, binders: (SqlPreparedStatement.() -> Unit)?): QueryResult<Long> =
    createOrGetStatement(identifier, sql).run {
      bind(parameters, binders)
      step()
      freemem()
      return QueryResult.Value(0)
    }

  private fun Statement.bind(parameters: Int, binders: (SqlPreparedStatement.() -> Unit)?) = binders?.let {
    if (parameters > 0) {
      val bound = JsSqlPreparedStatement(parameters)
      binders(bound)
      bind(bound.parameters.toTypedArray())
    }
  }

  private fun createOrGetStatement(identifier: Int?, sql: String): Statement = if (identifier == null) {
    db.prepare(sql)
  } else {
    statements.getOrPut(identifier, { db.prepare(sql) }).apply { reset() }
  }

  override fun newTransaction(): QueryResult<Transacter.Transaction> {
    val enclosing = transaction
    val transaction = Transaction(enclosing)
    this.transaction = transaction
    if (enclosing == null) {
      db.run("BEGIN TRANSACTION")
    }
    return QueryResult.Value(transaction)
  }

  override fun currentTransaction() = transaction

  override fun close() = db.close()

  private inner class Transaction(
    override val enclosingTransaction: Transacter.Transaction?,
  ) : Transacter.Transaction() {
    override fun endTransaction(successful: Boolean): QueryResult<Unit> {
      if (enclosingTransaction == null) {
        if (successful) {
          db.run("END TRANSACTION")
        } else {
          db.run("ROLLBACK TRANSACTION")
        }
      }
      transaction = enclosingTransaction
      return QueryResult.Unit
    }
  }
}

private class JsSqlCursor(private val statement: Statement) : SqlCursor {
  override fun next(): Boolean = statement.step()
  override fun getString(index: Int): String? = statement.get()[index]
  override fun getLong(index: Int): Long? = (statement.get()[index] as? Double)?.toLong()
  override fun getBytes(index: Int): ByteArray? = (statement.get()[index] as? Uint8Array)?.let {
    Int8Array(it.buffer).unsafeCast<ByteArray>()
  }
  override fun getDouble(index: Int): Double? = statement.get()[index]

  override fun getBoolean(index: Int): Boolean? {
    val double = (statement.get()[index] as? Double)
    return if (double == null) null
    else double.toLong() == 1L
  }

  fun close() { statement.freemem() }
}

internal class JsSqlPreparedStatement(parameters: Int) : SqlPreparedStatement {

  val parameters = MutableList<Any?>(parameters) { null }

  override fun bindBytes(index: Int, bytes: ByteArray?) {
    parameters[index] = bytes?.toTypedArray()
  }

  override fun bindLong(index: Int, long: Long?) {
    // We convert Long to Double because Kotlin's Double is mapped to JS number
    // whereas Kotlin's Long is implemented as a JS object
    parameters[index] = long?.toDouble()
  }

  override fun bindDouble(index: Int, double: Double?) {
    parameters[index] = double
  }

  override fun bindString(index: Int, string: String?) {
    parameters[index] = string
  }

  override fun bindBoolean(index: Int, boolean: Boolean?) {
    parameters[index] = when (boolean) {
      null -> null
      true -> 1.0
      false -> 0.0
    }
  }
}
