/*
 * Copyright (C) 2017 The Android Open Source Project
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

// Copied from https://github.com/cashapp/multiplatform-paging/blob/androidx-main/paging/paging-runtime/src/commonMain/kotlin/androidx/paging/NullPaddedListDiffHelper.kt

package androidx.paging

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import app.cash.paging.DiffingChangePayload.ITEM_TO_PLACEHOLDER
import app.cash.paging.DiffingChangePayload.PLACEHOLDER_POSITION_CHANGE
import app.cash.paging.DiffingChangePayload.PLACEHOLDER_TO_ITEM
import app.cash.paging.NullPaddedList

/**
 * Methods for computing and applying DiffResults between PagedLists.
 *
 * To minimize the amount of diffing caused by placeholders, we only execute DiffUtil in a reduced
 * 'diff space' - in the range (computeLeadingNulls..size-computeTrailingNulls).
 *
 * This allows the diff of a PagedList, e.g.:
 * 100 nulls, placeholder page, (empty page) x 5, page, 100 nulls
 *
 * To only inform DiffUtil about single loaded page in this case, by pruning all other nulls from
 * consideration.
 */
internal fun <T : Any> NullPaddedList<T>.computeDiff(
  newList: NullPaddedList<T>,
  diffCallback: DiffUtil.ItemCallback<T>,
): NullPaddedDiffResult {
  val oldSize = storageCount
  val newSize = newList.storageCount

  val diffResult = DiffUtil.calculateDiff(
    object : DiffUtil.Callback() {
      override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldItem = getFromStorage(oldItemPosition)
        val newItem = newList.getFromStorage(newItemPosition)

        return when {
          oldItem === newItem -> true
          else -> diffCallback.getChangePayload(oldItem, newItem)
        }
      }

      override fun getOldListSize() = oldSize

      override fun getNewListSize() = newSize

      override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = getFromStorage(oldItemPosition)
        val newItem = newList.getFromStorage(newItemPosition)

        return when {
          oldItem === newItem -> true
          else -> diffCallback.areItemsTheSame(oldItem, newItem)
        }
      }

      override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = getFromStorage(oldItemPosition)
        val newItem = newList.getFromStorage(newItemPosition)

        return when {
          oldItem === newItem -> true
          else -> diffCallback.areContentsTheSame(oldItem, newItem)
        }
      }
    },
    true,
  )
  // find first overlap
  val hasOverlap = (0 until storageCount).any {
    diffResult.convertOldPositionToNew(it) != RecyclerView.NO_POSITION
  }
  return NullPaddedDiffResult(
    diff = diffResult,
    hasOverlap = hasOverlap,
  )
}

private class OffsettingListUpdateCallback internal constructor(
  private val offset: Int,
  private val callback: ListUpdateCallback,
) : ListUpdateCallback {
  override fun onInserted(position: Int, count: Int) {
    callback.onInserted(position + offset, count)
  }

  override fun onRemoved(position: Int, count: Int) {
    callback.onRemoved(position + offset, count)
  }

  override fun onMoved(fromPosition: Int, toPosition: Int) {
    callback.onMoved(fromPosition + offset, toPosition + offset)
  }

  override fun onChanged(position: Int, count: Int, payload: Any?) {
    callback.onChanged(position + offset, count, payload)
  }
}

/**
 * See NullPaddedDiffing.md for how this works and why it works that way :).
 *
 * Note: if lists mutate between diffing the snapshot and dispatching the diff here, then we
 * handle this by passing the snapshot to the callback, and dispatching those changes
 * immediately after dispatching this diff.
 */
internal fun <T : Any> NullPaddedList<T>.dispatchDiff(
  callback: ListUpdateCallback,
  newList: NullPaddedList<T>,
  diffResult: NullPaddedDiffResult,
) {
  if (diffResult.hasOverlap) {
    OverlappingListsDiffDispatcher.dispatchDiff(
      oldList = this,
      newList = newList,
      callback = callback,
      diffResult = diffResult,
    )
  } else {
    // if no values overlapped between two lists, use change with payload *unless* the
    // position represents real items in both old and new lists in which case *change* would
    // be misleading hence we need to dispatch add/remove.
    DistinctListsDiffDispatcher.dispatchDiff(
      callback = callback,
      oldList = this,
      newList = newList,
    )
  }
}

/**
 * Given an oldPosition representing an anchor in the old data set, computes its new position
 * after the diff, or a guess if it no longer exists.
 */
internal fun NullPaddedList<*>.transformAnchorIndex(
  diffResult: NullPaddedDiffResult,
  newList: NullPaddedList<*>,
  oldPosition: Int,
): Int {
  if (!diffResult.hasOverlap) {
    // if lists didn't overlap, use old position
    return oldPosition.coerceIn(0 until newList.size)
  }
  // diffResult's indices starting after nulls, need to transform to diffutil indices
  // (see also dispatchDiff(), which adds this offset when dispatching)
  val diffIndex = oldPosition - placeholdersBefore

  val oldSize = storageCount

  // if our anchor is non-null, use it or close item's position in new list
  if (diffIndex in 0 until oldSize) {
    // search outward from old position for position that maps
    for (i in 0..29) {
      val positionToTry = diffIndex + i / 2 * if (i % 2 == 1) -1 else 1

      // reject if (null) item was not passed to DiffUtil, and wouldn't be in the result
      if (positionToTry < 0 || positionToTry >= storageCount) {
        continue
      }

      val result = diffResult.diff.convertOldPositionToNew(positionToTry)
      if (result != -1) {
        // also need to transform from diffutil output indices to newList
        return result + newList.placeholdersBefore
      }
    }
  }

  // not anchored to an item in new list, so just reuse position (clamped to newList size)
  return oldPosition.coerceIn(0 until newList.size)
}

internal class NullPaddedDiffResult(
  val diff: DiffUtil.DiffResult,
  // true if two lists have at least 1 item the same
  val hasOverlap: Boolean,
)

/**
 * Helper class to implement the heuristic documented in NullPaddedDiffing.md.
 */
internal object OverlappingListsDiffDispatcher {
  fun <T> dispatchDiff(
    oldList: NullPaddedList<T>,
    newList: NullPaddedList<T>,
    callback: ListUpdateCallback,
    diffResult: NullPaddedDiffResult,
  ) {
    val callbackWrapper = PlaceholderUsingUpdateCallback(
      oldList = oldList,
      newList = newList,
      callback = callback,
    )
    diffResult.diff.dispatchUpdatesTo(callbackWrapper)
    callbackWrapper.fixPlaceholders()
  }

  @Suppress("NOTHING_TO_INLINE")
  private class PlaceholderUsingUpdateCallback<T>(
    private val oldList: NullPaddedList<T>,
    private val newList: NullPaddedList<T>,
    private val callback: ListUpdateCallback,
  ) : ListUpdateCallback {
    // These variables hold the "current" value for placeholders and storage count and are
    // updated as we dispatch notify events to `callback`.
    private var placeholdersBefore = oldList.placeholdersBefore
    private var placeholdersAfter = oldList.placeholdersAfter
    private var storageCount = oldList.storageCount

    // Track if we used placeholders for a certain case to avoid using them for both additions
    // and removals at the same time, which might end up sending misleading change events.
    private var placeholdersBeforeState = UNUSED
    private var placeholdersAfterState = UNUSED

    /**
     * Offsets a value based on placeholders to make it suitable to pass into the callback.
     */
    private inline fun Int.offsetForDispatch() = this + placeholdersBefore

    fun fixPlaceholders() {
      // add / remove placeholders to match the new list
      fixLeadingPlaceholders()
      fixTrailingPlaceholders()
    }

    private fun fixTrailingPlaceholders() {
      // the #of placeholders that didn't have any updates. We might need to send position
      // change events for them if their original positions are no longer valid.
      var unchangedPlaceholders = minOf(oldList.placeholdersAfter, placeholdersAfter)

      val postPlaceholdersToAdd = newList.placeholdersAfter - placeholdersAfter
      val runningListSize = placeholdersBefore + storageCount + placeholdersAfter
      // check if unchanged placeholders changed their positions between two lists
      val unchangedPlaceholdersStartPos = runningListSize - unchangedPlaceholders
      val unchangedPlaceholdersMoved =
        unchangedPlaceholdersStartPos != (oldList.size - unchangedPlaceholders)
      if (postPlaceholdersToAdd > 0) {
        // always add to the end of the list
        callback.onInserted(runningListSize, postPlaceholdersToAdd)
      } else if (postPlaceholdersToAdd < 0) {
        // always remove from the end
        // notice that postPlaceholdersToAdd is negative, thats why it is added to
        // runningListEnd
        callback.onRemoved(
          runningListSize + postPlaceholdersToAdd,
          -postPlaceholdersToAdd,
        )
        // remove them from unchanged placeholders, notice that it is an addition because
        // postPlaceholdersToAdd is negative
        unchangedPlaceholders += postPlaceholdersToAdd
      }
      if (unchangedPlaceholders > 0 && unchangedPlaceholdersMoved) {
        // These placeholders didn't get any change event yet their list positions changed.
        // We should send an update as the position of a placeholder is part of its data.
        callback.onChanged(
          unchangedPlaceholdersStartPos,
          unchangedPlaceholders,
          PLACEHOLDER_POSITION_CHANGE,
        )
      }
      placeholdersAfter = newList.placeholdersAfter
    }

    private fun fixLeadingPlaceholders() {
      // the #of placeholders that didn't have any updates. We might need to send position
      // change events if we further modify the list.
      val unchangedPlaceholders = minOf(oldList.placeholdersBefore, placeholdersBefore)
      val prePlaceholdersToAdd = newList.placeholdersBefore - placeholdersBefore
      if (prePlaceholdersToAdd > 0) {
        if (unchangedPlaceholders > 0) {
          // these will be shifted down so send a change event for them
          callback.onChanged(0, unchangedPlaceholders, PLACEHOLDER_POSITION_CHANGE)
        }
        // always insert to the beginning of the list
        callback.onInserted(0, prePlaceholdersToAdd)
      } else if (prePlaceholdersToAdd < 0) {
        // always remove from the beginning of the list
        callback.onRemoved(0, -prePlaceholdersToAdd)
        if (unchangedPlaceholders + prePlaceholdersToAdd > 0) {
          // these have been shifted up, send a change event for them. We add the negative
          // number of `prePlaceholdersToAdd` not to send change events for them
          callback.onChanged(
            0,
            unchangedPlaceholders + prePlaceholdersToAdd,
            PLACEHOLDER_POSITION_CHANGE,
          )
        }
      }
      placeholdersBefore = newList.placeholdersBefore
    }

    override fun onInserted(position: Int, count: Int) {
      when {
        dispatchInsertAsPlaceholderAfter(position, count) -> {
          // dispatched as placeholders after
        }
        dispatchInsertAsPlaceholderBefore(position, count) -> {
          // dispatched as placeholders before
        }
        else -> {
          // not at the edge, dispatch as usual
          callback.onInserted(position.offsetForDispatch(), count)
        }
      }
      storageCount += count
    }

    /**
     * Return true if it is dispatched, false otherwise.
     */
    private fun dispatchInsertAsPlaceholderBefore(position: Int, count: Int): Boolean {
      if (position > 0) {
        return false // not at the edge
      }
      if (placeholdersBeforeState == USED_FOR_REMOVAL) {
        return false
      }
      val asPlaceholderChange = minOf(count, placeholdersBefore)
      if (asPlaceholderChange > 0) {
        placeholdersBeforeState = USED_FOR_ADDITION
        // this index is negative because we are going back. offsetForDispatch will fix it
        val index = (0 - asPlaceholderChange)
        callback.onChanged(index.offsetForDispatch(), asPlaceholderChange, PLACEHOLDER_TO_ITEM)
        placeholdersBefore -= asPlaceholderChange
      }
      val asInsert = count - asPlaceholderChange
      if (asInsert > 0) {
        callback.onInserted(0.offsetForDispatch(), asInsert)
      }
      return true
    }

    /**
     * Return true if it is dispatched, false otherwise.
     */
    private fun dispatchInsertAsPlaceholderAfter(position: Int, count: Int): Boolean {
      if (position < storageCount) {
        return false // not at the edge
      }
      if (placeholdersAfterState == USED_FOR_REMOVAL) {
        return false
      }
      val asPlaceholderChange = minOf(count, placeholdersAfter)
      if (asPlaceholderChange > 0) {
        placeholdersAfterState = USED_FOR_ADDITION
        callback.onChanged(position.offsetForDispatch(), asPlaceholderChange, PLACEHOLDER_TO_ITEM)
        placeholdersAfter -= asPlaceholderChange
      }
      val asInsert = count - asPlaceholderChange
      if (asInsert > 0) {
        callback.onInserted((position + asPlaceholderChange).offsetForDispatch(), asInsert)
      }
      return true
    }

    override fun onRemoved(position: Int, count: Int) {
      when {
        dispatchRemovalAsPlaceholdersAfter(position, count) -> {
          // dispatched as changed into placeholder
        }
        dispatchRemovalAsPlaceholdersBefore(position, count) -> {
          // dispatched as changed into placeholder
        }
        else -> {
          // fallback, need to handle here
          callback.onRemoved(position.offsetForDispatch(), count)
        }
      }
      storageCount -= count
    }

    /**
     * Return true if it is dispatched, false otherwise.
     */
    private fun dispatchRemovalAsPlaceholdersBefore(position: Int, count: Int): Boolean {
      if (position > 0) {
        return false
      }
      if (placeholdersBeforeState == USED_FOR_ADDITION) {
        return false
      }
      // see how many removals we can convert to change.
      // make sure we don't end up having too many placeholders that we'll end up removing
      // anyways
      val maxPlaceholdersToAdd = newList.placeholdersBefore - placeholdersBefore
      val asPlaceholders = minOf(maxPlaceholdersToAdd, count).coerceAtLeast(0)
      val asRemoval = count - asPlaceholders
      // first remove then use placeholders to make sure items that are closer to the loaded
      // content center are more likely to stay in the list
      if (asRemoval > 0) {
        callback.onRemoved(0.offsetForDispatch(), asRemoval)
      }
      if (asPlaceholders > 0) {
        placeholdersBeforeState = USED_FOR_REMOVAL
        callback.onChanged(0.offsetForDispatch(), asPlaceholders, ITEM_TO_PLACEHOLDER)
        placeholdersBefore += asPlaceholders
      }
      return true
    }

    /**
     * Return true if it is dispatched, false otherwise.
     */
    private fun dispatchRemovalAsPlaceholdersAfter(position: Int, count: Int): Boolean {
      val end = position + count
      if (end < storageCount) {
        return false // not at the edge
      }
      if (placeholdersAfterState == USED_FOR_ADDITION) {
        return false
      }
      // see how many removals we can convert to change.
      // make sure we don't end up having too many placeholders that we'll end up removing
      // anyways
      val maxPlaceholdersToAdd = newList.placeholdersAfter - placeholdersAfter
      val asPlaceholders = minOf(maxPlaceholdersToAdd, count).coerceAtLeast(0)
      val asRemoval = count - asPlaceholders
      // first use placeholders then removal to make sure items that are closer to
      // the loaded content center are more likely to stay in the list
      if (asPlaceholders > 0) {
        placeholdersAfterState = USED_FOR_REMOVAL
        callback.onChanged(position.offsetForDispatch(), asPlaceholders, ITEM_TO_PLACEHOLDER)
        placeholdersAfter += asPlaceholders
      }
      if (asRemoval > 0) {
        callback.onRemoved((position + asPlaceholders).offsetForDispatch(), asRemoval)
      }
      return true
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
      callback.onMoved(fromPosition.offsetForDispatch(), toPosition.offsetForDispatch())
    }

    override fun onChanged(position: Int, count: Int, payload: Any?) {
      callback.onChanged(position.offsetForDispatch(), count, payload)
    }

    companion object {
      // markers for edges to avoid using them for both additions and removals
      private const val UNUSED = 1
      private const val USED_FOR_REMOVAL = UNUSED + 1
      private const val USED_FOR_ADDITION = USED_FOR_REMOVAL + 1
    }
  }
}

/**
 * Helper object to dispatch diffs when two lists do not overlap at all.
 *
 * We try to send change events when an item's position is replaced with a placeholder or vice
 * versa.
 * If there is an item in a given position in before and after lists, we dispatch add/remove for
 * them not to trigger unexpected change animations.
 */
internal object DistinctListsDiffDispatcher {
  fun <T : Any> dispatchDiff(
    callback: ListUpdateCallback,
    oldList: NullPaddedList<T>,
    newList: NullPaddedList<T>,
  ) {
    val storageOverlapStart = maxOf(oldList.placeholdersBefore, newList.placeholdersBefore)
    val storageOverlapEnd = minOf(
      oldList.placeholdersBefore + oldList.storageCount,
      newList.placeholdersBefore + newList.storageCount,
    )
    // we need to dispatch add/remove for overlapping storage positions
    val overlappingStorageSize = storageOverlapEnd - storageOverlapStart
    if (overlappingStorageSize > 0) {
      callback.onRemoved(storageOverlapStart, overlappingStorageSize)
      callback.onInserted(storageOverlapStart, overlappingStorageSize)
    }
    // now everything else is good as a change animation.
    // make sure to send a change for old items whose positions are still in the list
    // to handle cases where there is no overlap, we min/max boundaries
    val changeEventStartBoundary = minOf(storageOverlapStart, storageOverlapEnd)
    val changeEventEndBoundary = maxOf(storageOverlapStart, storageOverlapEnd)
    dispatchChange(
      callback = callback,
      startBoundary = changeEventStartBoundary,
      endBoundary = changeEventEndBoundary,
      start = oldList.placeholdersBefore.coerceAtMost(newList.size),
      end = (oldList.placeholdersBefore + oldList.storageCount).coerceAtMost(newList.size),
      payload = ITEM_TO_PLACEHOLDER,
    )
    // now for new items that were mapping to placeholders, send change events
    dispatchChange(
      callback = callback,
      startBoundary = changeEventStartBoundary,
      endBoundary = changeEventEndBoundary,
      start = newList.placeholdersBefore.coerceAtMost(oldList.size),
      end = (newList.placeholdersBefore + newList.storageCount).coerceAtMost(oldList.size),
      payload = PLACEHOLDER_TO_ITEM,
    )
    // finally, fix the size
    val itemsToAdd = newList.size - oldList.size
    if (itemsToAdd > 0) {
      callback.onInserted(oldList.size, itemsToAdd)
    } else if (itemsToAdd < 0) {
      callback.onRemoved(oldList.size + itemsToAdd, -itemsToAdd)
    }
  }

  private fun dispatchChange(
    callback: ListUpdateCallback,
    startBoundary: Int,
    endBoundary: Int,
    start: Int,
    end: Int,
    payload: Any,
  ) {
    val beforeOverlapCount = startBoundary - start
    if (beforeOverlapCount > 0) {
      callback.onChanged(start, beforeOverlapCount, payload)
    }
    val afterOverlapCount = end - endBoundary
    if (afterOverlapCount > 0) {
      callback.onChanged(endBoundary, afterOverlapCount, payload)
    }
  }
}
