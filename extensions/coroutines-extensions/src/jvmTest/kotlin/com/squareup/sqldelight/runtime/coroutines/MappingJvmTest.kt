/*
 * Copyright (C) 2018 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.squareup.sqldelight.runtime.coroutines

import com.squareup.sqldelight.runtime.coroutines.Employee.Companion.MAPPER
import com.squareup.sqldelight.runtime.coroutines.Employee.Companion.SELECT_EMPLOYEES
import com.squareup.sqldelight.runtime.coroutines.TestDb.Companion.TABLE_EMPLOYEE
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineContext
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.test.assertEquals

@FlowPreview
@ObsoleteCoroutinesApi // Explicitly using a test context (dispatcher).
class MappingJvmTest {
  @get:Rule val timeout = Timeout(1, SECONDS)

  private val testContext = TestCoroutineContext()
  private lateinit var db: TestDb

  @Before fun setup() {
    db = TestDb()
  }

  @After fun tearDown() {
    db.close()
  }

  @Test fun mapToOneUsesContext() = runTest {
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", MAPPER)
        .asFlow()
        .mapToOne(testContext)
        .assertInitialAndAsyncNotificationUsesContext()
  }

  @Test fun mapToOneOrDefaultUsesContext() = runTest {
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", MAPPER)
        .asFlow()
        .mapToOneOrDefault(Employee("fred", "Fred Frederson"), testContext)
        .assertInitialAndAsyncNotificationUsesContext()
  }

  @Test fun mapToOneOrNullUsesContext() = runTest {
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", MAPPER)
        .asFlow()
        .mapToOneOrNull(testContext)
        .assertInitialAndAsyncNotificationUsesContext()
  }

  @Test fun mapToOneNonNullUsesContext() = runTest {
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", MAPPER)
        .asFlow()
        .mapToOneNotNull(testContext)
        .assertInitialAndAsyncNotificationUsesContext()
  }

  @Test fun mapToListUsesContext() = runTest {
    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, MAPPER)
        .asFlow()
        .mapToList(testContext)
        .assertInitialAndAsyncNotificationUsesContext()
  }

  private suspend fun Flow<*>.assertInitialAndAsyncNotificationUsesContext() = coroutineScope {
    var seen = 0

    // Using undispatched to ensure this reaches first suspension point.
    @Suppress("EXPERIMENTAL_API_USAGE")
    launch(testContext, start = UNDISPATCHED) {
      collect {
        if (++seen == 2) {
          throw CancellationException("done!")
        }
      }
    }

    assertEquals(0, seen)
    testContext.triggerActions()
    assertEquals(1, seen)

    db.employee(Employee("john", "John Johnson"))
    assertEquals(1, seen)
    testContext.triggerActions()
    assertEquals(2, seen)
  }
}
