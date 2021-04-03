package com.squareup.sqldelight.drivers.sqljs

import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.TransacterImpl
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.SqlPreparedStatement
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import kotlin.js.Promise

fun Promise<Database>.driver(): Promise<SqlDriver> = then { JsSqlDriver(it) }

fun Promise<SqlDriver>.withSchema(schema: SqlDriver.Schema? = null): Promise<SqlDriver> = then {
  schema?.create(it)
  it
}

fun Promise<SqlDriver>.transacter(): Promise<Transacter> = then { object : TransacterImpl(it) {} }

fun initSqlDriver(schema: SqlDriver.Schema? = null): Promise<SqlDriver> = initDb().driver().withSchema(schema)

class JsSqlDriver(private val db: Database) : SqlDriver {

  private val statements = mutableMapOf<Int, Statement>()
  private var transaction: Transacter.Transaction? = null

  override fun <R> executeQuery(
    identifier: Int?,
    sql: String,
    mapper: (SqlCursor) -> R,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?,
  ): R {
    val cursor = createOrGetStatement(identifier, sql).run {
      bind(binders)
      JsSqlCursor(this)
    }

    return try {
      mapper(cursor)
    } finally {
      cursor.close()
    }
  }

  override fun execute(identifier: Int?, sql: String, parameters: Int, binders: (SqlPreparedStatement.() -> Unit)?) =
    createOrGetStatement(identifier, sql).run {
      bind(binders)
      step()
      freemem()
    }

  private fun Statement.bind(binders: (SqlPreparedStatement.() -> Unit)?) = binders?.let {
    val bound = JsSqlPreparedStatement()
    binders(bound)
    bind(bound.parameters.toTypedArray())
  }

  private fun createOrGetStatement(identifier: Int?, sql: String): Statement = if (identifier == null) {
    db.prepare(sql)
  } else {
    statements.getOrPut(identifier, { db.prepare(sql) }).apply { reset() }
  }

  override fun newTransaction(): Transacter.Transaction {
    val enclosing = transaction
    val transaction = Transaction(enclosing)
    this.transaction = transaction
    if (enclosing == null) {
      db.run("BEGIN TRANSACTION")
    }
    return transaction
  }

  override fun currentTransaction() = transaction

  override fun close() = db.close()

  private inner class Transaction(
    override val enclosingTransaction: Transacter.Transaction?
  ) : Transacter.Transaction() {
    override fun endTransaction(successful: Boolean) {
      if (enclosingTransaction == null) {
        if (successful) {
          db.run("END TRANSACTION")
        } else {
          db.run("ROLLBACK TRANSACTION")
        }
      }
      transaction = enclosingTransaction
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
  fun close() { statement.freemem() }
}

private class JsSqlPreparedStatement : SqlPreparedStatement {

  val parameters = mutableListOf<Any?>()

  override fun bindBytes(index: Int, bytes: ByteArray?) {
    parameters.add(bytes?.toTypedArray())
  }

  override fun bindLong(index: Int, long: Long?) {
    // We convert Long to Double because Kotlin's Double is mapped to JS number
    // whereas Kotlin's Long is implemented as a JS object
    parameters.add(long?.toDouble())
  }

  override fun bindDouble(index: Int, double: Double?) {
    parameters.add(double)
  }

  override fun bindString(index: Int, string: String?) {
    parameters.add(string)
  }
}
