/*
 * Copyright (C) 2016 Square, Inc.
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

import androidx.paging.AsyncPagingDataDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import app.cash.paging.PagingData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle

private object NoopListCallback : ListUpdateCallback {
  override fun onChanged(position: Int, count: Int, payload: Any?) = Unit
  override fun onMoved(fromPosition: Int, toPosition: Int) = Unit
  override fun onInserted(position: Int, count: Int) = Unit
  override fun onRemoved(position: Int, count: Int) = Unit
}

@ExperimentalCoroutinesApi
fun <T : Any> PagingData<T>.withPagingDataDiffer(
  testScope: TestScope,
  diffCallback: DiffUtil.ItemCallback<T>,
  block: AsyncPagingDataDiffer<T>.() -> Unit,
) {
  val testDispatcher = UnconfinedTestDispatcher(testScope.testScheduler)
  val pagingDataDiffer = AsyncPagingDataDiffer(
    diffCallback,
    NoopListCallback,
    mainDispatcher = testDispatcher,
    workerDispatcher = testDispatcher,
  )
  val job = testScope.launch {
    pagingDataDiffer.submitData(this@withPagingDataDiffer)
  }
  testScope.advanceUntilIdle()
  block(pagingDataDiffer)
  job.cancel()
}
