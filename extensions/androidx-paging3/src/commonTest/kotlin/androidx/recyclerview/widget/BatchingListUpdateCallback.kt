/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.recyclerview.widget

// Copied from https://github.com/cashapp/multiplatform-paging/blob/androidx-main/paging/paging-runtime/src/commonMain/kotlin/androidx/recyclerview/widget/BatchingListUpdateCallback.kt

import kotlin.math.max
import kotlin.math.min

/**
 * Wraps a {@link ListUpdateCallback} callback and batches operations that can be merged.
 * <p>
 * For instance, when 2 add operations comes that adds 2 consecutive elements,
 * BatchingListUpdateCallback merges them and calls the wrapped callback only once.
 * <p>
 * This is a general purpose class and is also used by
 * {@link DiffUtil.DiffResult DiffResult} and
 * {@link SortedList} to minimize the number of updates that are dispatched.
 * <p>
 * If you use this class to batch updates, you must call {@link #dispatchLastEvent()} when the
 * stream of update events drain.
 */
class BatchingListUpdateCallback(callback: ListUpdateCallback) : ListUpdateCallback {
  private companion object {
    private val TYPE_NONE = 0
    private val TYPE_ADD = 1
    private val TYPE_REMOVE = 2
    private val TYPE_CHANGE = 3
  }

  val mWrapped: ListUpdateCallback = callback

  var mLastEventType = TYPE_NONE
  var mLastEventPosition = -1
  var mLastEventCount = -1
  var mLastEventPayload: Any? = null

  /**
   * BatchingListUpdateCallback holds onto the last event to see if it can be merged with the
   * next one. When stream of events finish, you should call this method to dispatch the last
   * event.
   */
  fun dispatchLastEvent() {
    if (mLastEventType == TYPE_NONE) {
      return
    }
    when (mLastEventType) {
      TYPE_ADD -> mWrapped.onInserted(mLastEventPosition, mLastEventCount)
      TYPE_REMOVE -> mWrapped.onRemoved(mLastEventPosition, mLastEventCount)
      TYPE_CHANGE -> mWrapped.onChanged(mLastEventPosition, mLastEventCount, mLastEventPayload)
    }
    mLastEventPayload = null
    mLastEventType = TYPE_NONE
  }

  override fun onInserted(position: Int, count: Int) {
    if (mLastEventType == TYPE_ADD && position >= mLastEventPosition &&
      position <= mLastEventPosition + mLastEventCount
    ) {
      mLastEventCount += count
      mLastEventPosition = min(position, mLastEventPosition)
      return
    }
    dispatchLastEvent()
    mLastEventPosition = position
    mLastEventCount = count
    mLastEventType = TYPE_ADD
  }

  override fun onRemoved(position: Int, count: Int) {
    if (mLastEventType == TYPE_REMOVE && mLastEventPosition >= position &&
      mLastEventPosition <= position + count
    ) {
      mLastEventCount += count
      mLastEventPosition = position
      return
    }
    dispatchLastEvent()
    mLastEventPosition = position
    mLastEventCount = count
    mLastEventType = TYPE_REMOVE
  }

  override fun onMoved(fromPosition: Int, toPosition: Int) {
    dispatchLastEvent() // moves are not merged
    mWrapped.onMoved(fromPosition, toPosition)
  }

  override fun onChanged(position: Int, count: Int, payload: Any?) {
    if (mLastEventType == TYPE_CHANGE &&
      !(
        position > mLastEventPosition + mLastEventCount ||
          position + count < mLastEventPosition || mLastEventPayload != payload
        )
    ) {
      // take potential overlap into account
      val previousEnd: Int = mLastEventPosition + mLastEventCount
      mLastEventPosition = min(position, mLastEventPosition)
      mLastEventCount = max(previousEnd, position + count) - mLastEventPosition
      return
    }
    dispatchLastEvent()
    mLastEventPosition = position
    mLastEventCount = count
    mLastEventPayload = payload
    mLastEventType = TYPE_CHANGE
  }
}
