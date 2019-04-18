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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineContext
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@FlowPreview
class ObservingJvmTest {
  private lateinit var db: TestDb

  @Before fun setup() {
    db = TestDb()
  }

  @After fun tearDown() {
    db.close()
  }

  @Ignore("Internal Channel and TestCoroutineContext interaction isn't working correctly")
  @ObsoleteCoroutinesApi // Explicitly using a test context (dispatcher).
  @Test fun queryInitialValueAndTriggerUsesScheduler() = runTest {
    val context = TestCoroutineContext()
    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, MAPPER)
        .asFlow(context)
        .test {
          noEvents()

          context.triggerActions()
          item().assert {
            hasRow("alice", "Alice Allison")
            hasRow("bob", "Bob Bobberson")
            hasRow("eve", "Eve Evenson")
          }

          db.employee(Employee("john", "John Johnson"))
          noEvents()

          context.triggerActions()
          item().assert {
            hasRow("alice", "Alice Allison")
            hasRow("bob", "Bob Bobberson")
            hasRow("eve", "Eve Evenson")
            hasRow("john", "John Johnson")
          }

          cancel()
        }
  }
}
