/*
 * Copyright 2019 The Android Open Source Project
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

// Copied from https://github.com/cashapp/multiplatform-paging/blob/androidx-main/paging/paging-runtime/src/commonMain/kotlin/androidx/paging/AsyncPagingDataDiffer.kt

@file:Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")

package androidx.paging

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import app.cash.paging.CombinedLoadStates
import app.cash.paging.DifferCallback
import app.cash.paging.ItemSnapshotList
import app.cash.paging.LoadType.REFRESH
import app.cash.paging.NullPaddedList
import app.cash.paging.PagingData
import app.cash.paging.PagingDataDiffer
import co.touchlab.stately.concurrency.AtomicInt
import kotlin.jvm.JvmOverloads
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Helper class for mapping a [PagingData] into a
 * [RecyclerView.Adapter][androidx.recyclerview.widget.RecyclerView.Adapter].
 *
 * For simplicity, [PagingDataAdapter] can often be used in place of this class.
 * [AsyncPagingDataDiffer] is exposed for complex cases, and where overriding [PagingDataAdapter] to
 * support paging isn't convenient.
 */
class AsyncPagingDataDiffer<T : Any> @JvmOverloads constructor(
  private val diffCallback: DiffUtil.ItemCallback<T>,
  @Suppress("ListenerLast") // have to suppress for each, due to defaults / JvmOverloads
  private val updateCallback: ListUpdateCallback,
  @Suppress("ListenerLast") // have to suppress for each, due to defaults / JvmOverloads
  private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
  @Suppress("ListenerLast") // have to suppress for each, due to defaults / JvmOverloads
  private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
  @Suppress("MemberVisibilityCanBePrivate") // synthetic access
  internal val differCallback = object : DifferCallback {
    override fun onInserted(position: Int, count: Int) {
      // Ignore if count == 0 as it makes this event a no-op.
      if (count > 0) {
        updateCallback.onInserted(position, count)
      }
    }

    override fun onRemoved(position: Int, count: Int) {
      // Ignore if count == 0 as it makes this event a no-op.
      if (count > 0) {
        updateCallback.onRemoved(position, count)
      }
    }

    override fun onChanged(position: Int, count: Int) {
      // Ignore if count == 0 as it makes this event a no-op.
      if (count > 0) {
        // NOTE: pass a null payload to convey null -> item, or item -> null
        updateCallback.onChanged(position, count, null)
      }
    }
  }

  /** True if we're currently executing [getItem] */
  @Suppress("MemberVisibilityCanBePrivate") // synthetic access
  internal var inGetItem: Boolean = false

  private val differBase = object : PagingDataDiffer<T>(differCallback, mainDispatcher) {
    override suspend fun presentNewList(
      previousList: NullPaddedList<T>,
      newList: NullPaddedList<T>,
      lastAccessedIndex: Int,
      onListPresentable: () -> Unit,
    ) = when {
      // fast path for no items -> some items
      previousList.size == 0 -> {
        onListPresentable()
        differCallback.onInserted(0, newList.size)
        null
      }
      // fast path for some items -> no items
      newList.size == 0 -> {
        onListPresentable()
        differCallback.onRemoved(0, previousList.size)
        null
      }
      else -> {
        val diffResult = withContext(workerDispatcher) {
          previousList.computeDiff(newList, diffCallback)
        }
        onListPresentable()
        previousList.dispatchDiff(updateCallback, newList, diffResult)
        previousList.transformAnchorIndex(
          diffResult = diffResult,
          newList = newList,
          oldPosition = lastAccessedIndex,
        )
      }
    }

    /**
     * Return if [getItem] is running to post any data modifications.
     *
     * This must be done because RecyclerView can't be modified during an onBind, when
     * [getItem] is generally called.
     */
    override fun postEvents(): Boolean {
      return inGetItem
    }
  }

  private val submitDataId = AtomicInt(0)

  /**
   * Present a [PagingData] until it is invalidated by a call to [refresh] or
   * [PagingSource.invalidate].
   *
   * This method is typically used when collecting from a [Flow][kotlinx.coroutines.flow.Flow]
   * produced by [Pager]. For RxJava or LiveData support, use the non-suspending overload of
   * [submitData], which accepts a [Lifecycle].
   *
   * Note: This method suspends while it is actively presenting page loads from a [PagingData],
   * until the [PagingData] is invalidated. Although cancellation will propagate to this call
   * automatically, collecting from a [Pager.flow] with the intention of presenting the most
   * up-to-date representation of your backing dataset should typically be done using
   * [collectLatest][kotlinx.coroutines.flow.collectLatest].
   *
   * @see [Pager]
   */
  suspend fun submitData(pagingData: PagingData<T>) {
    submitDataId.incrementAndGet()
    differBase.collectFrom(pagingData)
  }

  /**
   * Retry any failed load requests that would result in a [LoadState.Error] update to this
   * [AsyncPagingDataDiffer].
   *
   * Unlike [refresh], this does not invalidate [PagingSource], it only retries failed loads
   * within the same generation of [PagingData].
   *
   * [LoadState.Error] can be generated from two types of load requests:
   *  * [PagingSource.load] returning [PagingSource.LoadResult.Error]
   *  * [RemoteMediator.load] returning [RemoteMediator.MediatorResult.Error]
   */
  fun retry() {
    differBase.retry()
  }

  /**
   * Refresh the data presented by this [AsyncPagingDataDiffer].
   *
   * [refresh] triggers the creation of a new [PagingData] with a new instance of [PagingSource]
   * to represent an updated snapshot of the backing dataset. If a [RemoteMediator] is set,
   * calling [refresh] will also trigger a call to [RemoteMediator.load] with [LoadType] [REFRESH]
   * to allow [RemoteMediator] to check for updates to the dataset backing [PagingSource].
   *
   * Note: This API is intended for UI-driven refresh signals, such as swipe-to-refresh.
   * Invalidation due repository-layer signals, such as DB-updates, should instead use
   * [PagingSource.invalidate].
   *
   * @see PagingSource.invalidate
   *
   * @sample androidx.paging.samples.refreshSample
   */
  fun refresh() {
    differBase.refresh()
  }

  /**
   * Get the item from the current PagedList at the specified index.
   *
   * Note that this operates on both loaded items and null padding within the PagedList.
   *
   * @param index Index of item to get, must be >= 0, and < [itemCount]
   * @return The item, or `null`, if a `null` placeholder is at the specified position.
   */
  fun getItem(index: Int): T? {
    try {
      inGetItem = true
      return differBase[index]
    } finally {
      inGetItem = false
    }
  }

  /**
   * Returns the presented item at the specified position, without notifying Paging of the item
   * access that would normally trigger page loads.
   *
   * @param index Index of the presented item to return, including placeholders.
   * @return The presented item at position [index], `null` if it is a placeholder
   */
  fun peek(index: Int): T? {
    return differBase.peek(index)
  }

  /**
   * Returns a new [ItemSnapshotList] representing the currently presented items, including any
   * placeholders if they are enabled.
   */
  fun snapshot(): ItemSnapshotList<T> = differBase.snapshot()

  /**
   * Get the number of items currently presented by this Differ. This value can be directly
   * returned to [androidx.recyclerview.widget.RecyclerView.Adapter.getItemCount].
   *
   * @return Number of items being presented, including placeholders.
   */
  val itemCount: Int
    get() = differBase.size

  /**
   * A hot [Flow] of [CombinedLoadStates] that emits a snapshot whenever the loading state of the
   * current [PagingData] changes.
   *
   * This flow is conflated, so it buffers the last update to [CombinedLoadStates] and
   * immediately delivers the current load states on collection.
   *
   * @sample androidx.paging.samples.loadStateFlowSample
   */
  val loadStateFlow: Flow<CombinedLoadStates> = differBase.loadStateFlow

  /**
   * A hot [Flow] that emits after the pages presented to the UI are updated, even if the
   * actual items presented don't change.
   *
   * An update is triggered from one of the following:
   *   * [submitData] is called and initial load completes, regardless of any differences in
   *     the loaded data
   *   * A [Page][androidx.paging.PagingSource.LoadResult.Page] is inserted
   *   * A [Page][androidx.paging.PagingSource.LoadResult.Page] is dropped
   *
   * Note: This is a [SharedFlow][kotlinx.coroutines.flow.SharedFlow] configured to replay
   * 0 items with a buffer of size 64. If a collector lags behind page updates, it may
   * trigger multiple times for each intermediate update that was presented while your collector
   * was still working. To avoid this behavior, you can
   * [conflate][kotlinx.coroutines.flow.conflate] this [Flow] so that you only receive the latest
   * update, which is useful in cases where you are simply updating UI and don't care about
   * tracking the exact number of page updates.
   */
  val onPagesUpdatedFlow: Flow<Unit> = differBase.onPagesUpdatedFlow

  /**
   * Add a listener which triggers after the pages presented to the UI are updated, even if the
   * actual items presented don't change.
   *
   * An update is triggered from one of the following:
   *   * [submitData] is called and initial load completes, regardless of any differences in
   *     the loaded data
   *   * A [Page][androidx.paging.PagingSource.LoadResult.Page] is inserted
   *   * A [Page][androidx.paging.PagingSource.LoadResult.Page] is dropped
   *
   * @param listener called after pages presented are updated.
   *
   * @see removeOnPagesUpdatedListener
   */
  fun addOnPagesUpdatedListener(listener: () -> Unit) {
    differBase.addOnPagesUpdatedListener(listener)
  }

  /**
   * Remove a previously registered listener for new [PagingData] generations completing
   * initial load and presenting to the UI.
   *
   * @param listener Previously registered listener.
   *
   * @see addOnPagesUpdatedListener
   */
  fun removeOnPagesUpdatedListener(listener: () -> Unit) {
    differBase.removeOnPagesUpdatedListener(listener)
  }

  /**
   * Add a [CombinedLoadStates] listener to observe the loading state of the current [PagingData].
   *
   * As new [PagingData] generations are submitted and displayed, the listener will be notified to
   * reflect the current [CombinedLoadStates].
   *
   * @param listener [LoadStates] listener to receive updates.
   *
   * @see removeLoadStateListener
   *
   * @sample androidx.paging.samples.addLoadStateListenerSample
   */
  fun addLoadStateListener(listener: (CombinedLoadStates) -> Unit) {
    differBase.addLoadStateListener(listener)
  }

  /**
   * Remove a previously registered [CombinedLoadStates] listener.
   *
   * @param listener Previously registered listener.
   * @see addLoadStateListener
   */
  fun removeLoadStateListener(listener: (CombinedLoadStates) -> Unit) {
    differBase.removeLoadStateListener(listener)
  }
}
