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

// Copied from https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:paging/paging-runtime/src/main/java/androidx/paging/AsyncPagingDataDiffer.kt
// Removed unused Android-specific functions

package androidx.paging

import androidx.annotation.IntRange
import androidx.annotation.MainThread
import androidx.paging.LoadType.REFRESH
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmOverloads
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * Helper class for mapping a [PagingData] into a
 * [RecyclerView.Adapter][androidx.recyclerview.widget.RecyclerView.Adapter].
 *
 * For simplicity, [PagingDataAdapter] can often be used in place of this class.
 * [AsyncPagingDataDiffer] is exposed for complex cases, and where overriding [PagingDataAdapter] to
 * support paging isn't convenient.
 */
@OptIn(ExperimentalAtomicApi::class)
class AsyncPagingDataDiffer<T : Any>
/**
 * Construct an [AsyncPagingDataDiffer].
 *
 * @param diffCallback Callback for calculating the diff between two non-disjoint lists on
 *   [REFRESH]. Used as a fallback for item-level diffing when Paging is unable to find a faster
 *   path for generating the UI events required to display the new list.
 * @param updateCallback [ListUpdateCallback] which receives UI events dispatched by this
 *   [AsyncPagingDataDiffer] as items are loaded.
 * @param mainDispatcher [CoroutineContext] where UI events are dispatched. Typically, this should
 *   be [Dispatchers.Main].
 * @param workerDispatcher [CoroutineContext] where the work to generate UI events is dispatched,
 *   for example when diffing lists on [REFRESH]. Typically, this should dispatch on a background
 *   thread; [Dispatchers.Default] by default.
 */
@JvmOverloads
constructor(
  private val diffCallback: DiffUtil.ItemCallback<T>,
  @Suppress("ListenerLast") // have to suppress for each, due to optional args
  private val updateCallback: ListUpdateCallback,
  @Suppress("ListenerLast") // have to suppress for each, due to optional args
  private val mainDispatcher: CoroutineContext = Dispatchers.Main,
  @Suppress("ListenerLast") // have to suppress for each, due to optional args
  private val workerDispatcher: CoroutineContext = Dispatchers.Default,
) {
  /**
   * Construct an [AsyncPagingDataDiffer].
   *
   * @param diffCallback Callback for calculating the diff between two non-disjoint lists on
   *   [REFRESH]. Used as a fallback for item-level diffing when Paging is unable to find a faster
   *   path for generating the UI events required to display the new list.
   * @param updateCallback [ListUpdateCallback] which receives UI events dispatched by this
   *   [AsyncPagingDataDiffer] as items are loaded.
   * @param mainDispatcher [CoroutineDispatcher] where UI events are dispatched. Typically, this
   *   should be [Dispatchers.Main].
   */
  @Deprecated(
    message = "Superseded by constructors which accept CoroutineContext",
    level = DeprecationLevel.HIDDEN,
  )
  // Only for binary compatibility; cannot apply @JvmOverloads as the function signature would
  // conflict with the primary constructor.
  @Suppress("MissingJvmstatic")
  constructor(
    diffCallback: DiffUtil.ItemCallback<T>,
    // have to suppress for each, due to optional args
    @Suppress("ListenerLast")
    updateCallback: ListUpdateCallback,
    // have to suppress for each, due to optional args
    @Suppress("ListenerLast")
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
  ) : this(
    diffCallback = diffCallback,
    updateCallback = updateCallback,
    mainDispatcher = mainDispatcher,
    workerDispatcher = Dispatchers.Default,
  )

  /**
   * Construct an [AsyncPagingDataDiffer].
   *
   * @param diffCallback Callback for calculating the diff between two non-disjoint lists on
   *   [REFRESH]. Used as a fallback for item-level diffing when Paging is unable to find a faster
   *   path for generating the UI events required to display the new list.
   * @param updateCallback [ListUpdateCallback] which receives UI events dispatched by this
   *   [AsyncPagingDataDiffer] as items are loaded.
   * @param mainDispatcher [CoroutineDispatcher] where UI events are dispatched. Typically, this
   *   should be [Dispatchers.Main].
   * @param workerDispatcher [CoroutineDispatcher] where the work to generate UI events is
   *   dispatched, for example when diffing lists on [REFRESH]. Typically, this should dispatch on
   *   a background thread; [Dispatchers.Default] by default.
   */
  @Deprecated(
    message = "Superseded by constructors which accept CoroutineContext",
    level = DeprecationLevel.HIDDEN,
  )
  // Only for binary compatibility; cannot apply @JvmOverloads as the function signature would
  // conflict with the primary constructor.
  @Suppress("MissingJvmstatic")
  constructor(
    diffCallback: DiffUtil.ItemCallback<T>,
    // have to suppress for each, due to optional args
    @Suppress("ListenerLast")
    updateCallback: ListUpdateCallback,
    // have to suppress for each, due to optional args
    @Suppress("ListenerLast")
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    // have to suppress for each, due to optional args
    @Suppress("ListenerLast")
    workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
  ) : this(
    diffCallback = diffCallback,
    updateCallback = updateCallback,
    mainDispatcher = mainDispatcher,
    workerDispatcher = workerDispatcher,
  )

  /** True if we're currently executing [getItem] */
  internal val inGetItem = MutableStateFlow(false)
  private var lastAccessedIndex = 0

  /**
   * When presenter presents a new Refresh load, temporarily stores the previous generation of
   * loaded data while doing [computeDiff] to ensure that RV has access to the previous list (the
   * list actually still presented on the UI) for diffing.
   *
   * This presenter should be null when not computing diff.
   */
  private val previousPresenter: AtomicReference<PlaceholderPaddedList<T>?> = AtomicReference(null)

  internal val presenter =
    object : PagingDataPresenter<T>(mainDispatcher) {
      override suspend fun presentPagingDataEvent(event: PagingDataEvent<T>) {
        when (event) {
          is PagingDataEvent.Refresh ->
            event.apply {
              when {
                // fast path for no items -> some items
                previousList.size == 0 -> {
                  if (newList.size > 0) {
                    updateCallback.onInserted(0, newList.size)
                  }
                }
                // fast path for some items -> no items
                newList.size == 0 -> {
                  if (previousList.size > 0) {
                    updateCallback.onRemoved(0, previousList.size)
                  }
                }
                else -> {
                  previousPresenter.store(previousList)
                  val diffResult =
                    try {
                      withContext(workerDispatcher) {
                        previousList.computeDiff(newList, diffCallback)
                      }
                    } finally {
                      // Set null here to ensure previousPresenter is reset
                      // even if this refresh is interrupted.
                      // Also, ensure we reset presenter on main thread to
                      // avoid potential race with RV doing work between
                      // set null and dispatchDiff.
                      previousPresenter.store(null)
                    }

                  previousList.dispatchDiff(updateCallback, newList, diffResult)
                  val transformedIndex =
                    previousList.transformAnchorIndex(
                      diffResult = diffResult,
                      newList = newList,
                      oldPosition = lastAccessedIndex,
                    )
                  // Transform the last loadAround index from the old list to the
                  // new list by passing it through the DiffResult, and pass
                  // it forward as a ViewportHint within the new list to the
                  // next generation of Pager.
                  // This ensures prefetch distance for the last ViewportHint from
                  // the old list is respected in the new list, even if
                  // invalidation interrupts the prepend / append load that
                  // would have fulfilled it in the old list.
                  lastAccessedIndex = transformedIndex
                  get(transformedIndex)
                }
              }
            }
          /**
           * For each [PagingDataEvent.Prepend] or [PagingDataEvent.Append] there are
           * three potential events handled in the following order:
           * 1) change this covers any placeholder/item conversions, and is done first
           * 2) item insert/remove this covers any remaining items that are
           *    inserted/removed, but aren't swapping with placeholders
           * 3) placeholder insert/remove after the above, placeholder count can be wrong
           *    for a number of reasons - approximate counting or filtering are the most
           *    common. In either case, we adjust placeholders at the far end of the list,
           *    so that they don't trigger animations near the user.
           */
          is PagingDataEvent.Prepend ->
            event.apply {
              val insertSize = inserted.size

              val placeholdersChangedCount = minOf(oldPlaceholdersBefore, insertSize)
              val placeholdersChangedPos =
                oldPlaceholdersBefore - placeholdersChangedCount
              val itemsInsertedCount = insertSize - placeholdersChangedCount
              val itemsInsertedPos = 0

              // ... then trigger callbacks, so callbacks won't see inconsistent state
              if (placeholdersChangedCount > 0) {
                updateCallback.onChanged(
                  placeholdersChangedPos,
                  placeholdersChangedCount,
                  null,
                )
              }
              if (itemsInsertedCount > 0) {
                updateCallback.onInserted(itemsInsertedPos, itemsInsertedCount)
              }
              val placeholderInsertedCount =
                newPlaceholdersBefore - oldPlaceholdersBefore +
                  placeholdersChangedCount
              if (placeholderInsertedCount > 0) {
                updateCallback.onInserted(0, placeholderInsertedCount)
              } else if (placeholderInsertedCount < 0) {
                updateCallback.onRemoved(0, -placeholderInsertedCount)
              }
            }
          is PagingDataEvent.Append ->
            event.apply {
              val insertSize = inserted.size
              val placeholdersChangedCount = minOf(oldPlaceholdersAfter, insertSize)
              val placeholdersChangedPos = startIndex
              val itemsInsertedCount = insertSize - placeholdersChangedCount
              val itemsInsertedPos = placeholdersChangedPos + placeholdersChangedCount

              if (placeholdersChangedCount > 0) {
                updateCallback.onChanged(
                  placeholdersChangedPos,
                  placeholdersChangedCount,
                  null,
                )
              }
              if (itemsInsertedCount > 0) {
                updateCallback.onInserted(itemsInsertedPos, itemsInsertedCount)
              }
              val placeholderInsertedCount =
                newPlaceholdersAfter - oldPlaceholdersAfter +
                  placeholdersChangedCount
              val newTotalSize = startIndex + insertSize + newPlaceholdersAfter
              if (placeholderInsertedCount > 0) {
                updateCallback.onInserted(
                  newTotalSize - placeholderInsertedCount,
                  placeholderInsertedCount,
                )
              } else if (placeholderInsertedCount < 0) {
                updateCallback.onRemoved(newTotalSize, -placeholderInsertedCount)
              }
            }
          /**
           * For [PagingDataEvent.DropPrepend] or [PagingDataEvent.DropAppend] events
           * there are two potential events handled in the following order
           * 1) placeholder insert/remove We first adjust placeholders at the far end of
           *    the list, so that they don't trigger animations near the user.
           * 2) change this covers any placeholder/item conversions, and is done after
           *    placeholders are trimmed/inserted to match new expected size
           *
           * Note: For drops we never run DiffUtil because it is safe to assume that empty
           * pages can never become non-empty no matter what transformations they go
           * through. [ListUpdateCallback] events generated by this helper always drop
           * contiguous sets of items because pages that depend on multiple
           * originalPageOffsets will always be the next closest page that's non-empty.
           */
          is PagingDataEvent.DropPrepend ->
            event.apply {
              // Trim or insert placeholders to match expected newSize.
              val placeholdersToInsert =
                newPlaceholdersBefore - dropCount - oldPlaceholdersBefore
              if (placeholdersToInsert > 0) {
                updateCallback.onInserted(0, placeholdersToInsert)
              } else if (placeholdersToInsert < 0) {
                updateCallback.onRemoved(0, -placeholdersToInsert)
              }
              // Compute the index of the first item that must be rebound as a
              // placeholder.
              // If any placeholders were inserted above, we only need to send
              // onChanged for the next
              // n = (newPlaceholdersBefore - placeholdersToInsert) items. E.g., if
              // two nulls
              // were inserted above, then the onChanged event can start from index =
              // 2.
              // Note: In cases where more items were dropped than there were
              // previously placeholders,
              // we can simply rebind n = newPlaceholdersBefore items starting from
              // position = 0.
              val firstItemIndex =
                maxOf(0, oldPlaceholdersBefore + placeholdersToInsert)
              // Compute the number of previously loaded items that were dropped and
              // now need to be
              // updated to null. This computes the distance between firstItemIndex
              // (inclusive),
              // and index of the last leading placeholder (inclusive) in the final
              // list.
              val changeCount = newPlaceholdersBefore - firstItemIndex
              if (changeCount > 0) {
                updateCallback.onChanged(firstItemIndex, changeCount, null)
              }
            }
          is PagingDataEvent.DropAppend ->
            event.apply {
              val placeholdersToInsert =
                newPlaceholdersAfter - dropCount - oldPlaceholdersAfter
              val newSize = startIndex + newPlaceholdersAfter
              if (placeholdersToInsert > 0) {
                updateCallback.onInserted(
                  newSize - placeholdersToInsert,
                  placeholdersToInsert,
                )
              } else if (placeholdersToInsert < 0) {
                updateCallback.onRemoved(newSize, -placeholdersToInsert)
              }

              // Number of trailing placeholders in the list, before dropping, that
              // were
              // removed above during size adjustment.
              val oldPlaceholdersRemoved =
                when {
                  placeholdersToInsert < 0 ->
                    minOf(oldPlaceholdersAfter, -placeholdersToInsert)
                  else -> 0
                }
              // Compute the number of previously loaded items that were dropped and
              // now need
              // to be updated to null. This subtracts the total number of existing
              // placeholders in the list, before dropping, that were not removed
              // above
              // during size adjustment, from the total number of expected
              // placeholders.
              val changeCount =
                newPlaceholdersAfter - oldPlaceholdersAfter + oldPlaceholdersRemoved
              if (changeCount > 0) {
                updateCallback.onChanged(startIndex, changeCount, null)
              }
            }
        }
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
    submitDataId.incrementAndFetch()
    presenter.collectFrom(pagingData)
  }

  /**
   * Retry any failed load requests that would result in a [LoadState.Error] update to this
   * [AsyncPagingDataDiffer].
   *
   * Unlike [refresh], this does not invalidate [PagingSource], it only retries failed loads
   * within the same generation of [PagingData].
   *
   * [LoadState.Error] can be generated from two types of load requests:
   * * [PagingSource.load] returning [PagingSource.LoadResult.Error]
   * * [RemoteMediator.load] returning [RemoteMediator.MediatorResult.Error]
   */
  fun retry() {
    presenter.retry()
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
   * @sample androidx.paging.samples.refreshSample
   * @see PagingSource.invalidate
   */
  fun refresh() {
    presenter.refresh()
  }

  /**
   * Get the item from the current PagedList at the specified index.
   *
   * Note that this operates on both loaded items and null padding within the PagedList.
   *
   * @param index Index of item to get, must be >= 0, and < [itemCount]
   * @return The item, or `null`, if a `null` placeholder is at the specified position.
   */
  @MainThread
  fun getItem(@IntRange(from = 0) index: Int): T? {
    try {
      inGetItem.update { true }
      lastAccessedIndex = index
      val tempList = previousPresenter.load()
      return if (tempList != null) tempList.get(index) else presenter[index]
    } finally {
      inGetItem.update { false }
    }
  }

  /**
   * Returns the presented item at the specified position, without notifying Paging of the item
   * access that would normally trigger page loads.
   *
   * @param index Index of the presented item to return, including placeholders.
   * @return The presented item at position [index], `null` if it is a placeholder
   */
  @MainThread
  fun peek(@IntRange(from = 0) index: Int): T? {
    val tempList = previousPresenter.load()
    return if (tempList != null) tempList.peek(index) else presenter.peek(index)
  }

  /**
   * Returns a new [ItemSnapshotList] representing the currently presented items, including any
   * placeholders if they are enabled.
   */
  fun snapshot(): ItemSnapshotList<T> = previousPresenter.load()?.snapshot() ?: presenter.snapshot()

  /**
   * Get the number of items currently presented by this Differ. This value can be directly
   * returned to [androidx.recyclerview.widget.RecyclerView.Adapter.getItemCount].
   *
   * @return Number of items being presented, including placeholders.
   */
  val itemCount: Int
    get() = previousPresenter.load()?.size ?: presenter.size
}

private fun <T : Any> PlaceholderPaddedList<T>.get(@IntRange(from = 0) index: Int): T? {
  if (index < 0 || index >= size) {
    throw IndexOutOfBoundsException("Index: $index, Size: $size")
  }
  val localIndex = index - placeholdersBefore
  if (localIndex < 0 || localIndex >= dataCount) return null
  return getItem(localIndex)
}

private fun <T : Any> PlaceholderPaddedList<T>.peek(@IntRange(from = 0) index: Int): T? = get(index)

private fun <T : Any> PlaceholderPaddedList<T>.snapshot(): ItemSnapshotList<T> {
  val itemEndIndex = dataCount - 1
  val items = mutableListOf<T>()
  for (i in 0..itemEndIndex) {
    items.add(getItem(i))
  }
  return ItemSnapshotList(placeholdersBefore, placeholdersAfter, items)
}
