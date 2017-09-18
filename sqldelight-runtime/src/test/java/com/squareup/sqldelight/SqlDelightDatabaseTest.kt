package com.squareup.sqldelight

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.db.SupportSQLiteOpenHelper
import android.arch.persistence.db.framework.FrameworkSQLiteOpenHelperFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
class SqlDelightDatabaseTest {
    private val callback = object : SupportSQLiteOpenHelper.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) = Unit

    override fun onUpgrade(db: SupportSQLiteDatabase?, oldVersion: Int, newVersion: Int) = Unit
  }

  private val configuration = SupportSQLiteOpenHelper.Configuration.builder(RuntimeEnvironment.application)
      .callback(callback)
      .build()

  private val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
  private val database = object : SqlDelightDatabase(helper) { }

  @Test fun afterTransactionRunsAfterSuccessfulTransactionEnds() {
    val counter = AtomicInteger(0)
    database.newTransaction().use {
      database.afterTransaction { counter.incrementAndGet() }
      assertThat(counter.get()).isEqualTo(0)
      it.markSuccessful()
      assertThat(counter.get()).isEqualTo(0)
    }

    assertThat(counter.get()).isEqualTo(1)
  }

  @Test fun afterTransactionDoesNotRunAfterUnsuccessfulTransactionEnds() {
    val counter = AtomicInteger(0)
    database.newTransaction().use {
      database.afterTransaction { counter.incrementAndGet() }
      assertThat(counter.get()).isEqualTo(0)
    }

    assertThat(counter.get()).isEqualTo(0)
  }

  @Test fun afterTransactionRunsImmediatelyWithNoTransaction() {
    val counter = AtomicInteger(0)
    database.afterTransaction { counter.incrementAndGet() }
    assertThat(counter.get()).isEqualTo(1)
  }

  @Test fun afterTransactionRunsAfterParentTransactionEnds() {
    val counter = AtomicInteger(0)
    database.newTransaction().use {
      database.afterTransaction { counter.incrementAndGet() }
      assertThat(counter.get()).isEqualTo(0)

      database.newTransaction().use {
        database.afterTransaction { counter.incrementAndGet() }
        assertThat(counter.get()).isEqualTo(0)
        it.markSuccessful()
        assertThat(counter.get()).isEqualTo(0)
      }

      assertThat(counter.get()).isEqualTo(0)
      it.markSuccessful()
      assertThat(counter.get()).isEqualTo(0)
    }

    assertThat(counter.get()).isEqualTo(2)
  }
}
