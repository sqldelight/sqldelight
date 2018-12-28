package com.squareup.sqldelight.drivers.ios

import co.touchlab.sqliter.Statement
import co.touchlab.sqliter.bindBlob
import co.touchlab.sqliter.bindDouble
import co.touchlab.sqliter.bindLong
import co.touchlab.sqliter.bindString
import co.touchlab.stately.collections.frozenHashMap
import co.touchlab.stately.concurrency.ThreadLocalRef
import co.touchlab.stately.concurrency.value
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlPreparedStatement

internal class SqliterPreparedStatement(
  private val identifier: Int?,
  private val sql: String,
  private val db: ConnectionWrapper
) : SqlPreparedStatement {
  internal val dbStatement = ThreadLocalRef<Statement>()

  override fun bindBytes(index: Int, value: ByteArray?) {
    myStatement {
      bindBlob(index, value)
    }
  }

  override fun bindLong(index: Int, value: Long?) {
    myStatement {
      bindLong(index, value)
    }
  }

  override fun bindDouble(index: Int, value: Double?) {
    myStatement {
      bindDouble(index, value)
    }
  }

  override fun bindString(index: Int, value: String?) {
    myStatement {
      bindString(index, value)
    }
  }

  override fun executeQuery(): SqlCursor {
    throw AssertionError()
  }

  override fun execute() {
    myStatement {
      execute()
      resetStatement()
      db.accessConnection(false) {
        safePut(identifier, this@myStatement)
      }
      dbStatement.remove()
    }
  }

  private fun myStatement(block: Statement.() -> Unit) {
    if (dbStatement.value == null) {
      val stmt = db.accessConnection(false) {
        this.removeCreateStatement(identifier, sql)
      }
      dbStatement.value = stmt
    }

    val stat = dbStatement.value!!
    try {
      stat.block()
    } catch (e: Throwable) {
      dbStatement.remove()
      stat.finalizeStatement()
      throw e
    }
  }
}

internal class QueryPreparedStatement(
  private val identifier: Int?,
  private val sql: String,
  private val db: ConnectionWrapper
) : SqlPreparedStatement {
  private val binds = frozenHashMap<Int, (Statement) -> Unit>()

  override fun bindBytes(index: Int, value: ByteArray?) {
    binds[index] = { it.bindBlob(index, value) }
  }

  override fun bindLong(index: Int, value: Long?) {
    binds[index] = { it.bindLong(index, value) }
  }

  override fun bindDouble(index: Int, value: Double?) {
    binds[index] = { it.bindDouble(index, value) }
  }

  override fun bindString(index: Int, value: String?) {
    binds[index] = { it.bindString(index, value) }
  }

  override fun executeQuery(): SqlCursor {
    return db.accessConnection(true) {
      val statement = removeCreateStatement(identifier, sql)
      try {
        binds.forEach { it.value(statement) }
        val cursor = statement.query()
        SqliterSqlCursor(cursor, trackCursor(cursor, identifier))
      } catch (e: Exception) {
        statement.finalizeStatement()
        throw e
      }
    }
  }

  override fun execute() {
    throw AssertionError()
  }
}