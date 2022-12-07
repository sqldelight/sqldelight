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

// Copied from https://github.com/cashapp/multiplatform-paging/blob/androidx-main/paging/paging-runtime/src/commonMain/kotlin/androidx/recyclerview/widget/ListUpdateCallback.kt

package androidx.recyclerview.widget

/**
 * An interface that can receive Update operations that are applied to a list.
 * <p>
 * This class can be used together with DiffUtil to detect changes between two lists.
 */
interface ListUpdateCallback {
  /**
   * Called when {@code count} number of items are inserted at the given position.
   *
   * @param position The position of the new item.
   * @param count    The number of items that have been added.
   */
  fun onInserted(position: Int, count: Int)

  /**
   * Called when {@code count} number of items are removed from the given position.
   *
   * @param position The position of the item which has been removed.
   * @param count    The number of items which have been removed.
   */
  fun onRemoved(position: Int, count: Int)

  /**
   * Called when an item changes its position in the list.
   *
   * @param fromPosition The previous position of the item before the move.
   * @param toPosition   The new position of the item.
   */
  fun onMoved(fromPosition: Int, toPosition: Int)

  /**
   * Called when {@code count} number of items are updated at the given position.
   *
   * @param position The position of the item which has been updated.
   * @param count    The number of items which has changed.
   */
  fun onChanged(position: Int, count: Int, payload: Any?)
}
