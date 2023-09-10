package com.squareup.sqldelight.drivers.sqljs

import app.cash.sqldelight.driver.sqljs.Database
import app.cash.sqldelight.driver.sqljs.initDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.await
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class SqlJsTest {

  private fun testing(action: suspend CoroutineScope.(Database) -> Unit) = runTest {
    val db = initDb().await()
    db.run(
      """
      |CREATE TABLE test (
      |  id INTEGER PRIMARY KEY,
      |  value TEXT
      |);
      """.trimMargin(),
    )
    db.run(
      """
      |CREATE TABLE nullability_test (
      |  id INTEGER PRIMARY KEY,
      |  integer_value INTEGER,
      |  text_value TEXT,
      |  blob_value BLOB,
      |  real_value REAL
      |);
      """.trimMargin(),
    )
    action(db)
    db.close()
  }

  @Test fun insert_can_run_multiple_times() = testing { db ->

    val insert = "INSERT INTO test VALUES (?, ?);"
    val query = "SELECT * FROM test"
    val changes = "SELECT changes()"

    db.prepare(query).run {
      assertFalse(step())
      free()
    }

    db.prepare(insert).run {
      bind(dynamicArrayOf(1, "Alec"))
      step()
      free()
    }

    db.prepare(query).run {
      assertTrue(step())
      assertFalse(step())
      free()
    }

    db.prepare(changes).run {
      assertTrue(step())
      assertEquals(1, get()[0])
      free()
    }

    db.prepare(query).run {
      assertTrue(step())
      get().run {
        assertEquals(1, get(0))
        assertEquals("Alec", get(1))
      }
      free()
    }

    db.prepare(insert).run {
      bind(dynamicArrayOf(2, "Jake"))
      step()
      free()
    }

    db.prepare(changes).run {
      assertTrue(step())
      assertEquals(1, get()[0])
      free()
    }

    db.prepare(query).run {
      assertTrue(step())
      get().run {
        assertEquals(1, get(0))
        assertEquals("Alec", get(1))
      }
      assertTrue(step())
      get().run {
        assertEquals(2, get(0))
        assertEquals("Jake", get(1))
      }
      free()
    }

    db.prepare("DELETE FROM test").run {
      step()
      free()
    }

    db.prepare(changes).run {
      assertTrue(step())
      assertEquals(2, get()[0])
      free()
    }

    db.prepare(query).run {
      assertFalse(step())
      free()
    }
  }

  @Test fun query_can_run_multiple_times() = testing { db ->

    val insert = "INSERT INTO test VALUES (?, ?);"
    val changes = "SELECT changes()"

    val params = listOf(
      dynamicArrayOf(1, "Alec"),
      dynamicArrayOf(2, "Jake"),
    )

    params.forEach { param ->
      db.prepare(insert).run {
        bind(param)
        step()
        free()
      }
      db.prepare(changes).run {
        assertTrue(step())
        assertEquals(1, get()[0])
        free()
      }
    }

    val query = "SELECT * FROM test WHERE value = ?"

    repeat(2) {
      db.prepare(query, dynamicArrayOf("Jake")).run {
        assertTrue(step())
        get().run {
          assertEquals(2, get(0))
          assertEquals("Jake", get(1))
        }
        assertFalse(step())
        free()
      }
    }
  }

  @Test fun sqlResultSet_getters_return_null_if_the_column_values_are_NULL() = testing { db ->

    val insert = "INSERT INTO nullability_test VALUES (?, ?, ?, ?, ?);"
    val changes = "SELECT changes()"
    val select = "SELECT * FROM nullability_test"

    val changesStatement = db.prepare(changes)

    db.prepare(insert).run {
      bind(dynamicArrayOf(1, null, null, null, null))
      step()
      free()
    }

    changesStatement.run {
      step()
      assertEquals(1, get()[0])
      step()
      free()
    }

    db.prepare(select).run {
      assertTrue(step())
      get().run {
        assertEquals(1, get(0))
        assertNull(get(1))
        assertNull(get(2))
        assertNull(get(3))
        assertNull(get(4))
      }
      free()
    }
  }
}

fun dynamicArrayOf(vararg args: Any?): Array<dynamic> = args.map { it?.asDynamic() }.toTypedArray()
