/*
 * Copyright 2021 The Android Open Source Project
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
package app.cash.sqldelight.paging3

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit

/**
 * Makes refresh runnable accessible in tests. Used for LimitOffsetPagingSource unit tests that
 * needs to block InvalidationTracker's invalidation notification
 */
val InvalidationTracker.refreshRunnableForTest: Runnable
  get() = this.refreshRunnable

/**
 * True if invalidation tracker is pending a refresh event to get database changes.
 */
val InvalidationTracker.pendingRefreshForTest
  get() = this.pendingRefresh.get()

/**
 * Polls [InvalidationTracker] until it sets its pending refresh flag to true.
 */
suspend fun InvalidationTracker.awaitPendingRefresh() {
  withTimeout(TimeUnit.SECONDS.toMillis(3)) {
    while (true) {
      if (pendingRefreshForTest) return@withTimeout
      delay(50)
    }
  }
}
