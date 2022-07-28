
// Copyright Square, Inc.
package app.cash.sqldelight.paging3

import androidx.paging.AsyncPagingDataDiffer
import androidx.paging.PagingData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle

private object NoopListCallback : ListUpdateCallback {
  override fun onChanged(position: Int, count: Int, payload: Any?) {}
  override fun onMoved(fromPosition: Int, toPosition: Int) {}
  override fun onInserted(position: Int, count: Int) {}
  override fun onRemoved(position: Int, count: Int) {}
}

@ExperimentalCoroutinesApi
fun <T : Any> PagingData<T>.withPagingDataDiffer(
  testScope: TestScope,
  diffCallback: DiffUtil.ItemCallback<T>,
  block: AsyncPagingDataDiffer<T>.() -> Unit,
) {
  val pagingDataDiffer = AsyncPagingDataDiffer(
    diffCallback,
    NoopListCallback,
    workerDispatcher = Dispatchers.Main
  )
  val job = testScope.launch {
    pagingDataDiffer.submitData(this@withPagingDataDiffer)
  }
  testScope.advanceUntilIdle()
  block(pagingDataDiffer)
  job.cancel()
}
