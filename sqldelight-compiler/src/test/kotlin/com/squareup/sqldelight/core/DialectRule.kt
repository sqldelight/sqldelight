package com.squareup.sqldelight.core

import com.alecstrong.sql.psi.core.DialectPreset
import com.alecstrong.sql.psi.core.DialectPreset.HSQL
import com.alecstrong.sql.psi.core.DialectPreset.MYSQL
import com.alecstrong.sql.psi.core.DialectPreset.POSTGRESQL
import com.alecstrong.sql.psi.core.DialectPreset.SQLITE_3_18
import com.alecstrong.sql.psi.core.DialectPreset.SQLITE_3_24
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

annotation class DialectTest(
  val dialects: Array<DialectPreset>
)

class DialectRule : TestRule {
  lateinit var dialect: DialectPreset
    private set

  val textType: String
    get() = when (dialect) {
      SQLITE_3_18, SQLITE_3_24, MYSQL, HSQL, POSTGRESQL -> "TEXT"
    }

  override fun apply(base: Statement, description: Description): Statement {
    val annotation = description.getAnnotation(DialectTest::class.java)
        ?: return base
    return object : Statement() {
      override fun evaluate() {
        annotation.dialects.forEach { dialect ->
          this@DialectRule.dialect = dialect
          try {
            base.evaluate()
          } catch (t: Throwable) {
            throw AssertionError("Failed for $dialect", t)
          }
        }
      }
    }
  }
}
