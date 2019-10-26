package com.squareup.sqldelight.drivers.sqljs

import com.squareup.sqldelight.TransacterImpl
import com.squareup.sqldelight.db.SqlDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class JsTransacterTest : CoroutineScope by GlobalScope {
    protected lateinit var transacter: TransacterImpl
    private lateinit var driver: SqlDriver

    suspend fun setupDatabase(schema: SqlDriver.Schema): SqlDriver {
        val sql = initSql()
        val db = sql.Database()
        val driver = JsSqlDriver(db)
        schema.create(driver)
        return driver
    }

    suspend fun setup() {
        val driver = setupDatabase(object : SqlDriver.Schema {
            override val version = 1
            override fun create(driver: SqlDriver) {}
            override fun migrate(
                driver: SqlDriver,
                oldVersion: Int,
                newVersion: Int
            ) {
            }
        })
        transacter = object : TransacterImpl(driver) {}
        this.driver = driver
    }

    fun teardown() {
        driver.close()
    }

    @JsName("afterCommit_runs_after_transaction_commits")
    @Test fun `afterCommit runs after transaction commits`() = runTest {
        setup()

        var counter = 0
        transacter.transaction {
            afterCommit { counter++ }
            assertEquals(0, counter)
        }

        assertEquals(1, counter)

        teardown()
    }

    @JsName("afterCommit_does_not_run_after_transaction_rollbacks")
    @Test fun `afterCommit does not run after transaction rollbacks`() = runTest {
        setup()

        var counter = 0
        transacter.transaction {
            afterCommit { counter++ }
            assertEquals(0, counter)
            rollback()
        }

        assertEquals(0, counter)

        teardown()
    }

    @JsName("afterCommit_runs_after_enclosing_transaction_commits")
    @Test fun `afterCommit runs after enclosing transaction commits`() = runTest {
        setup()

        var counter = 0
        transacter.transaction {
            afterCommit { counter++ }
            assertEquals(0, counter)

            transaction {
                afterCommit { counter++ }
                assertEquals(0, counter)
            }

            assertEquals(0, counter)
        }

        assertEquals(2, counter)

        teardown()
    }

    @JsName("afterCommit_does_not_run_in_nested_transaction_when_enclosing_rolls_back")
    @Test fun `afterCommit does not run in nested transaction when enclosing rolls back`() = runTest {
        setup()

        var counter = 0
        transacter.transaction {
            afterCommit { counter++ }
            assertEquals(0, counter)

            transaction {
                afterCommit { counter++ }
            }

            rollback()
        }

        assertEquals(0, counter)

        teardown()
    }

    @JsName("afterCommit_does_not_run_in_nested_transaction_when_nested_rolls_back")
    @Test fun `afterCommit does not run in nested transaction when nested rolls back`() = runTest {
        setup()

        var counter = 0
        transacter.transaction {
            afterCommit { counter++ }
            assertEquals(0, counter)

            transaction {
                afterCommit { counter++ }
                rollback()
            }

            throw AssertionError()
        }

        assertEquals(0, counter)

        teardown()
    }

    @JsName("afterRollback_no_ops_if_the_transaction_never_rolls_back")
    @Test fun `afterRollback no-ops if the transaction never rolls back`() = runTest {
        setup()

        var counter = 0
        transacter.transaction {
            afterRollback { counter++ }
        }

        assertEquals(0, counter)

        teardown()
    }

    @JsName("afterRollback_runs_after_a_rollback_occurs")
    @Test fun `afterRollback runs after a rollback occurs`() = runTest {
        setup()

        var counter = 0
        transacter.transaction {
            afterRollback { counter++ }
            rollback()
        }

        assertEquals(1, counter)

        teardown()
    }

    @JsName("afterRollback_runs_after_an_inner_transaction_rolls_back")
    @Test fun `afterRollback runs after an inner transaction rolls back`() = runTest {
        setup()

        var counter = 0
        transacter.transaction {
            afterRollback { counter++ }
            transaction {
                rollback()
            }
            throw AssertionError()
        }

        assertEquals(1, counter)

        teardown()
    }

    @JsName("afterRollback_runs_in_an_inner_transaction_when_the_outer_transaction_rolls_back")
    @Test fun `afterRollback runs in an inner transaction when the outer transaction rolls back`() = runTest {
        setup()

        var counter = 0
        transacter.transaction {
            transaction {
                afterRollback { counter++ }
            }
            rollback()
        }

        assertEquals(1, counter)

        teardown()
    }

    @JsName("transactions_close_themselves_out_properly")
    @Test fun `transactions close themselves out properly`() = runTest {
        setup()

        var counter = 0
        transacter.transaction {
            afterCommit { counter++ }
        }

        transacter.transaction {
            afterCommit { counter++ }
        }

        assertEquals(2, counter)

        teardown()
    }

    @JsName("setting_no_enclosing_fails_if_there_is_a_currently_running_transaction")
    @Test fun `setting no enclosing fails if there is a currently running transaction`() = runTest {
        setup()

        transacter.transaction(noEnclosing = true) {
            try {
                transacter!!.transaction(noEnclosing = true) {
                    throw AssertionError()
                }
                throw AssertionError()
            } catch (e: IllegalStateException) {
                // Expected error.
            }
        }
    }

    @JsName("An_exception_thrown_in_postRollback_function_is_combined_with_the_exception_in_the_main_body")
    @Test fun `An exception thrown in postRollback function is combined with the exception in the main body`() = runTest {
        setup()

        class ExceptionA : RuntimeException()
        class ExceptionB : RuntimeException()
        try {
            transacter.transaction {
                afterRollback {
                    throw ExceptionA()
                }
                throw ExceptionB()
            }
            fail("Should have thrown!")
        } catch (e: Throwable) {
            assertTrue("Exception thrown in body not in message($e)") { e.toString().contains("ExceptionA") }
            assertTrue("Exception thrown in rollback not in message($e)") { e.toString().contains("ExceptionB") }
        } finally {
            teardown()
        }
    }
}
