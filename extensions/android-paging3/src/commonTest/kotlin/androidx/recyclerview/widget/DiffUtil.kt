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

// Copied from https://github.com/cashapp/multiplatform-paging/blob/androidx-main/paging/paging-runtime/src/commonMain/kotlin/androidx/recyclerview/widget/DiffUtil.kt

package androidx.recyclerview.widget

import kotlin.math.abs
import kotlin.math.min

/**
 * DiffUtil is a utility class that calculates the difference between two lists and outputs a
 * list of update operations that converts the first list into the second one.
 * <p>
 * It can be used to calculate updates for a RecyclerView Adapter. See {@link ListAdapter} and
 * {@link AsyncListDiffer} which can simplify the use of DiffUtil on a background thread.
 * <p>
 * DiffUtil uses Eugene W. Myers's difference algorithm to calculate the minimal number of updates
 * to convert one list into another. Myers's algorithm does not handle items that are moved so
 * DiffUtil runs a second pass on the result to detect items that were moved.
 * <p>
 * Note that DiffUtil, {@link ListAdapter}, and {@link AsyncListDiffer} require the list to not
 * mutate while in use.
 * This generally means that both the lists themselves and their elements (or at least, the
 * properties of elements used in diffing) should not be modified directly. Instead, new lists
 * should be provided any time content changes. It's common for lists passed to DiffUtil to share
 * elements that have not mutated, so it is not strictly required to reload all data to use
 * DiffUtil.
 * <p>
 * If the lists are large, this operation may take significant time so you are advised to run this
 * on a background thread, get the {@link DiffResult} then apply it on the RecyclerView on the main
 * thread.
 * <p>
 * This algorithm is optimized for space and uses O(N) space to find the minimal
 * number of addition and removal operations between the two lists. It has O(N + D^2) expected time
 * performance where D is the length of the edit script.
 * <p>
 * If move detection is enabled, it takes an additional O(MN) time where M is the total number of
 * added items and N is the total number of removed items. If your lists are already sorted by
 * the same constraint (e.g. a created timestamp for a list of posts), you can disable move
 * detection to improve performance.
 * <p>
 * The actual runtime of the algorithm significantly depends on the number of changes in the list
 * and the cost of your comparison methods. Below are some average run times for reference:
 * (The test list is composed of random UUID Strings and the tests are run on Nexus 5X with M)
 * <ul>
 *     <li>100 items and 10 modifications: avg: 0.39 ms, median: 0.35 ms
 *     <li>100 items and 100 modifications: 3.82 ms, median: 3.75 ms
 *     <li>100 items and 100 modifications without moves: 2.09 ms, median: 2.06 ms
 *     <li>1000 items and 50 modifications: avg: 4.67 ms, median: 4.59 ms
 *     <li>1000 items and 50 modifications without moves: avg: 3.59 ms, median: 3.50 ms
 *     <li>1000 items and 200 modifications: 27.07 ms, median: 26.92 ms
 *     <li>1000 items and 200 modifications without moves: 13.54 ms, median: 13.36 ms
 * </ul>
 * <p>
 * Due to implementation constraints, the max size of the list can be 2^26.
 *
 * @see ListAdapter
 * @see AsyncListDiffer
 */
object DiffUtil {

  private val DIAGONAL_COMPARATOR: Comparator<Diagonal> = object : Comparator<Diagonal> {
    override fun compare(o1: Diagonal, o2: Diagonal): Int {
      return o1.x - o2.x
    }
  }

  // Myers' algorithm uses two lists as axis labels. In DiffUtil's implementation, `x` axis is
  // used for old list and `y` axis is used for new list.

  /**
   * Calculates the list of update operations that can covert one list into the other one.
   *
   * @param cb The callback that acts as a gateway to the backing list data
   * @return A DiffResult that contains the information about the edit sequence to convert the
   * old list into the new list.
   */
  fun calculateDiff(cb: Callback): DiffResult {
    return calculateDiff(cb, true)
  }

  /**
   * Calculates the list of update operations that can covert one list into the other one.
   * <p>
   * If your old and new lists are sorted by the same constraint and items never move (swap
   * positions), you can disable move detection which takes <code>O(N^2)</code> time where
   * N is the number of added, moved, removed items.
   *
   * @param cb The callback that acts as a gateway to the backing list data
   * @param detectMoves True if DiffUtil should try to detect moved items, false otherwise.
   *
   * @return A DiffResult that contains the information about the edit sequence to convert the
   * old list into the new list.
   */
  fun calculateDiff(cb: Callback, detectMoves: Boolean): DiffResult {
    val oldSize: Int = cb.getOldListSize()
    val newSize: Int = cb.getNewListSize()

    val diagonals = mutableListOf<Diagonal>()

    // instead of a recursive implementation, we keep our own stack to avoid potential stack
    // overflow exceptions
    val stack = mutableListOf<Range>()

    stack.add(Range(0, oldSize, 0, newSize))

    val max: Int = (oldSize + newSize + 1) / 2
    // allocate forward and backward k-lines. K lines are diagonal lines in the matrix. (see the
    // paper for details)
    // These arrays lines keep the max reachable position for each k-line.
    val forward = CenteredArray(max * 2 + 1)
    val backward = CenteredArray(max * 2 + 1)

    // We pool the ranges to avoid allocations for each recursive call.
    val rangePool = mutableListOf<Range>()
    while (!stack.isEmpty()) {
      val range: Range = stack.removeAt(stack.size - 1)
      val snake: Snake? = midPoint(range, cb, forward, backward)
      if (snake != null) {
        // if it has a diagonal, save it
        if (snake.diagonalSize() > 0) {
          diagonals.add(snake.toDiagonal())
        }
        // add new ranges for left and right
        val left: Range = if (rangePool.isEmpty()) Range() else rangePool.removeAt(rangePool.size - 1)
        left.oldListStart = range.oldListStart
        left.newListStart = range.newListStart
        left.oldListEnd = snake.startX
        left.newListEnd = snake.startY
        stack.add(left)

        // re-use range for right
        //noinspection UnnecessaryLocalVariable
        val right: Range = range
        right.oldListEnd = range.oldListEnd
        right.newListEnd = range.newListEnd
        right.oldListStart = snake.endX
        right.newListStart = snake.endY
        stack.add(right)
      } else {
        rangePool.add(range)
      }
    }
    // sort snakes
    diagonals.sortWith(DIAGONAL_COMPARATOR)

    return DiffResult(cb, diagonals, forward.backingData(), backward.backingData(), detectMoves)
  }

  /**
   * Finds a middle snake in the given range.
   */
  private fun midPoint(
    range: Range,
    cb: Callback,
    forward: CenteredArray,
    backward: CenteredArray,
  ): Snake? {
    if (range.oldSize() < 1 || range.newSize() < 1) {
      return null
    }
    val max: Int = (range.oldSize() + range.newSize() + 1) / 2
    forward.set(1, range.oldListStart)
    backward.set(1, range.oldListEnd)
    repeat(max) { d ->
      var snake: Snake? = forward(range, cb, forward, backward, d)
      if (snake != null) {
        return snake
      }
      snake = backward(range, cb, forward, backward, d)
      if (snake != null) {
        return snake
      }
    }
    return null
  }

  private fun forward(
    range: Range,
    cb: Callback,
    forward: CenteredArray,
    backward: CenteredArray,
    d: Int,
  ): Snake? {
    val checkForSnake: Boolean = abs(range.oldSize() - range.newSize()) % 2 == 1
    val delta: Int = range.oldSize() - range.newSize()
    var k: Int = -d
    while (k <= d) {
      // we either come from d-1, k-1 OR d-1. k+1
      // as we move in steps of 2, array always holds both current and previous d values
      // k = x - y and each array value holds the max X, y = x - k
      val startX: Int
      val startY: Int
      var x: Int
      var y: Int
      if (k == -d || (k != d && forward.get(k + 1) > forward.get(k - 1))) {
        // picking k + 1, incrementing Y (by simply not incrementing X)
        startX = forward.get(k + 1)
        x = startX
      } else {
        // picking k - 1, incrementing X
        startX = forward.get(k - 1)
        x = startX + 1
      }
      y = range.newListStart + (x - range.oldListStart) - k
      startY = if (d == 0 || x != startX) y else y - 1
      // now find snake size
      while (x < range.oldListEnd &&
        y < range.newListEnd &&
        cb.areItemsTheSame(x, y)
      ) {
        x++
        y++
      }
      // now we have furthest reaching x, record it
      forward.set(k, x)
      if (checkForSnake) {
        // see if we did pass over a backwards array
        // mapping function: delta - k
        val backwardsK: Int = delta - k
        // if backwards K is calculated and it passed me, found match
        if (backwardsK >= -d + 1 &&
          backwardsK <= d - 1 &&
          backward.get(backwardsK) <= x
        ) {
          // match
          val snake = Snake()
          snake.startX = startX
          snake.startY = startY
          snake.endX = x
          snake.endY = y
          snake.reverse = false
          return snake
        }
      }
      k += 2
    }
    return null
  }

  private fun backward(
    range: Range,
    cb: Callback,
    forward: CenteredArray,
    backward: CenteredArray,
    d: Int,
  ): Snake? {
    val checkForSnake: Boolean = (range.oldSize() - range.newSize()) % 2 == 0
    val delta: Int = range.oldSize() - range.newSize()
    // same as forward but we go backwards from end of the lists to be beginning
    // this also means we'll try to optimize for minimizing x instead of maximizing it
    var k: Int = -d
    while (k <= d) {
      // we either come from d-1, k-1 OR d-1, k+1
      // as we move in steps of 2, array always holds both current and previous d values
      // k = x - y and each array value holds the MIN X, y = x - k
      // when x's are equal, we prioritize deletion over insertion
      val startX: Int
      val startY: Int
      var x: Int
      var y: Int

      if (k == -d || (k != d && backward.get(k + 1) < backward.get(k - 1))) {
        // picking k + 1, decrementing Y (by simply not decrementing X)
        startX = backward.get(k + 1)
        x = startX
      } else {
        // picking k - 1, decrementing X
        startX = backward.get(k - 1)
        x = startX - 1
      }
      y = range.newListEnd - ((range.oldListEnd - x) - k)
      startY = if (d == 0 || x != startX) y else y + 1
      // now find snake size
      while (x > range.oldListStart &&
        y > range.newListStart &&
        cb.areItemsTheSame(x - 1, y - 1)
      ) {
        x--
        y--
      }
      // now we have furthest point, record it (min X)
      backward.set(k, x)
      if (checkForSnake) {
        // see if we did pass over a backwards array
        // mapping function: delta - k
        val forwardsK: Int = delta - k
        // if forwards K is calculated and it passed me, found match
        if (forwardsK >= -d &&
          forwardsK <= d &&
          forward.get(forwardsK) >= x
        ) {
          // match
          val snake = Snake()
          // assignment are reverse since we are a reverse snake
          snake.startX = x
          snake.startY = y
          snake.endX = startX
          snake.endY = startY
          snake.reverse = true
          return snake
        }
      }
      k += 2
    }
    return null
  }

  /**
   * A Callback class used by DiffUtil while calculating the diff between two lists.
   */
  abstract class Callback {
    /**
     * Returns the size of the old list.
     *
     * @return The size of the old list.
     */
    abstract fun getOldListSize(): Int

    /**
     * Returns the size of the new list.
     *
     * @return The size of the new list.
     */
    abstract fun getNewListSize(): Int

    /**
     * Called by the DiffUtil to decide whether two object represent the same Item.
     * <p>
     * For example, if your items have unique ids, this method should check their id equality.
     *
     * @param oldItemPosition The position of the item in the old list
     * @param newItemPosition The position of the item in the new list
     * @return True if the two items represent the same object or false if they are different.
     */
    abstract fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean

    /**
     * Called by the DiffUtil when it wants to check whether two items have the same data.
     * DiffUtil uses this information to detect if the contents of an item has changed.
     * <p>
     * DiffUtil uses this method to check equality instead of {@link Object#equals(Object)}
     * so that you can change its behavior depending on your UI.
     * For example, if you are using DiffUtil with a
     * {@link RecyclerView.Adapter RecyclerView.Adapter}, you should
     * return whether the items' visual representations are the same.
     * <p>
     * This method is called only if {@link #areItemsTheSame(int, int)} returns
     * {@code true} for these items.
     *
     * @param oldItemPosition The position of the item in the old list
     * @param newItemPosition The position of the item in the new list which replaces the
     *                        oldItem
     * @return True if the contents of the items are the same or false if they are different.
     */
    abstract fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean

    /**
     * When {@link #areItemsTheSame(int, int)} returns {@code true} for two items and
     * {@link #areContentsTheSame(int, int)} returns false for them, DiffUtil
     * calls this method to get a payload about the change.
     * <p>
     * For example, if you are using DiffUtil with {@link RecyclerView}, you can return the
     * particular field that changed in the item and your
     * {@link RecyclerView.ItemAnimator ItemAnimator} can use that
     * information to run the correct animation.
     * <p>
     * Default implementation returns {@code null}.
     *
     * @param oldItemPosition The position of the item in the old list
     * @param newItemPosition The position of the item in the new list
     * @return A payload object that represents the change between the two items.
     */
    open fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
      return null
    }
  }

  /**
   * Callback for calculating the diff between two non-null items in a list.
   * <p>
   * {@link Callback} serves two roles - list indexing, and item diffing. ItemCallback handles
   * just the second of these, which allows separation of code that indexes into an array or List
   * from the presentation-layer and content specific diffing code.
   *
   * @param <T> Type of items to compare.
   */
  abstract class ItemCallback<T : Any> {
    /**
     * Called to check whether two objects represent the same item.
     * <p>
     * For example, if your items have unique ids, this method should check their id equality.
     * <p>
     * Note: {@code null} items in the list are assumed to be the same as another {@code null}
     * item and are assumed to not be the same as a non-{@code null} item. This callback will
     * not be invoked for either of those cases.
     *
     * @param oldItem The item in the old list.
     * @param newItem The item in the new list.
     * @return True if the two items represent the same object or false if they are different.
     * @see Callback#areItemsTheSame(int, int)
     */
    abstract fun areItemsTheSame(oldItem: T, newItem: T): Boolean

    /**
     * Called to check whether two items have the same data.
     * <p>
     * This information is used to detect if the contents of an item have changed.
     * <p>
     * This method to check equality instead of {@link Object#equals(Object)} so that you can
     * change its behavior depending on your UI.
     * <p>
     * For example, if you are using DiffUtil with a
     * {@link RecyclerView.Adapter RecyclerView.Adapter}, you should
     * return whether the items' visual representations are the same.
     * <p>
     * This method is called only if {@link #areItemsTheSame(T, T)} returns {@code true} for
     * these items.
     * <p>
     * Note: Two {@code null} items are assumed to represent the same contents. This callback
     * will not be invoked for this case.
     *
     * @param oldItem The item in the old list.
     * @param newItem The item in the new list.
     * @return True if the contents of the items are the same or false if they are different.
     * @see Callback#areContentsTheSame(int, int)
     */
    abstract fun areContentsTheSame(oldItem: T, newItem: T): Boolean

    /**
     * When {@link #areItemsTheSame(T, T)} returns {@code true} for two items and
     * {@link #areContentsTheSame(T, T)} returns false for them, this method is called to
     * get a payload about the change.
     * <p>
     * For example, if you are using DiffUtil with {@link RecyclerView}, you can return the
     * particular field that changed in the item and your
     * {@link RecyclerView.ItemAnimator ItemAnimator} can use that
     * information to run the correct animation.
     * <p>
     * Default implementation returns {@code null}.
     *
     * @see Callback#getChangePayload(int, int)
     */
    @Suppress("unused", "UNUSED_PARAMETER")
    fun getChangePayload(oldItem: T, newItem: T): Any? {
      return null
    }
  }

  /**
   * A diagonal is a match in the graph.
   * Rather than snakes, we only record the diagonals in the path.
   */
  class Diagonal(
    val x: Int,
    val y: Int,
    val size: Int,
  ) {

    fun endX(): Int {
      return x + size
    }

    fun endY(): Int {
      return y + size
    }
  }

  /**
   * Snakes represent a match between two lists. It is optionally prefixed or postfixed with an
   * add or remove operation. See the Myers' paper for details.
   */
  @Suppress("WeakerAccess")
  class Snake {
    /**
     * Position in the old list
     */
    var startX = 0

    /**
     * Position in the new list
     */
    var startY = 0

    /**
     * End position in the old list, exclusive
     */
    var endX = 0

    /**
     * End position in the new list, exclusive
     */
    var endY = 0

    /**
     * True if this snake was created in the reverse search, false otherwise.
     */
    var reverse = false

    fun hasAdditionOrRemoval(): Boolean {
      return endY - startY != endX - startX
    }

    fun isAddition(): Boolean {
      return endY - startY > endX - startX
    }

    fun diagonalSize(): Int {
      return min(endX - startX, endY - startY)
    }

    /**
     * Extract the diagonal of the snake to make reasoning easier for the rest of the
     * algorithm where we try to produce a path and also find moves.
     */
    fun toDiagonal(): Diagonal {
      if (hasAdditionOrRemoval()) {
        if (reverse) {
          // snake edge it at the end
          return Diagonal(startX, startY, diagonalSize())
        } else {
          // snake edge it at the beginning
          if (isAddition()) {
            return Diagonal(startX, startY + 1, diagonalSize())
          } else {
            return Diagonal(startX + 1, startY, diagonalSize())
          }
        }
      } else {
        // we are a pure diagonal
        return Diagonal(startX, startY, endX - startX)
      }
    }
  }

  /**
   * Represents a range in two lists that needs to be solved.
   * <p>
   * This internal class is used when running Myers' algorithm without recursion.
   * <p>
   * Ends are exclusive
   */
  class Range(
    var oldListStart: Int = 0,
    var oldListEnd: Int = 0,
    var newListStart: Int = 0,
    var newListEnd: Int = 0,
  ) {

    fun oldSize(): Int {
      return oldListEnd - oldListStart
    }

    fun newSize(): Int {
      return newListEnd - newListStart
    }
  }

  /**
   * This class holds the information about the result of a
   * {@link DiffUtil#calculateDiff(Callback, boolean)} call.
   * <p>
   * You can consume the updates in a DiffResult via
   * {@link #dispatchUpdatesTo(ListUpdateCallback)} or directly stream the results into a
   * {@link RecyclerView.Adapter} via {@link #dispatchUpdatesTo(RecyclerView.Adapter)}.
   */

  /**
   * @param callback        The callback that was used to calculate the diff
   * @param diagonals       Matches between the two lists
   * @param oldItemStatuses An int[] that can be re-purposed to keep metadata
   * @param newItemStatuses An int[] that can be re-purposed to keep metadata
   * @param detectMoves     True if this DiffResult will try to detect moved items
   */
  class DiffResult(
    // The callback that was given to calculate diff method.
    val mCallback: Callback,
    // The diagonals extracted from The Myers' snakes.
    val mDiagonals: MutableList<Diagonal>,
    // The list to keep oldItemStatuses. As we traverse old items, we assign flags to them
    // which also includes whether they were a real removal or a move (and its new index).
    val mOldItemStatuses: IntArray,
    // The list to keep newItemStatuses. As we traverse new items, we assign flags to them
    // which also includes whether they were a real addition or a move(and its old index).
    val mNewItemStatuses: IntArray,
    val mDetectMoves: Boolean,
  ) {

    companion object {

      /**
       * Signifies an item not present in the list.
       */
      val NO_POSITION: Int = -1

      /**
       * While reading the flags below, keep in mind that when multiple items move in a list,
       * Myers's may pick any of them as the anchor item and consider that one NOT_CHANGED while
       * picking others as additions and removals. This is completely fine as we later detect
       * all moves.
       * <p>
       * Below, when an item is mentioned to stay in the same "location", it means we won't
       * dispatch a move/add/remove for it, it DOES NOT mean the item is still in the same
       * position.
       */
      // item stayed the same.
      private val FLAG_NOT_CHANGED: Int = 1

      // item stayed in the same location but changed.
      private val FLAG_CHANGED: Int = FLAG_NOT_CHANGED shl 1

      // Item has moved and also changed.
      private val FLAG_MOVED_CHANGED: Int = FLAG_CHANGED shl 1

      // Item has moved but did not change.
      private val FLAG_MOVED_NOT_CHANGED: Int = FLAG_MOVED_CHANGED shl 1

      // Item moved
      private val FLAG_MOVED: Int = FLAG_MOVED_CHANGED or FLAG_MOVED_NOT_CHANGED

      // since we are re-using the int arrays that were created in the Myers' step, we mask
      // change flags
      private val FLAG_OFFSET: Int = 4

      private val FLAG_MASK: Int = (1 shl FLAG_OFFSET) - 1
    }

    private val mOldListSize: Int

    private val mNewListSize: Int

    init {
      mOldItemStatuses.fill(0)
      mNewItemStatuses.fill(0)
      mOldListSize = mCallback.getOldListSize()
      mNewListSize = mCallback.getNewListSize()
      addEdgeDiagonals()
      findMatchingItems()
    }

    /**
     * Add edge diagonals so that we can iterate as long as there are diagonals w/o lots of
     * null checks around
     */
    private fun addEdgeDiagonals() {
      val first: Diagonal? = mDiagonals.firstOrNull()
      // see if we should add 1 to the 0,0
      if (first == null || first.x != 0 || first.y != 0) {
        mDiagonals.add(0, Diagonal(0, 0, 0))
      }
      // always add one last
      mDiagonals.add(Diagonal(mOldListSize, mNewListSize, 0))
    }

    /**
     * Find position mapping from old list to new list.
     * If moves are requested, we'll also try to do an n^2 search between additions and
     * removals to find moves.
     */
    private fun findMatchingItems() {
      for (diagonal in mDiagonals) {
        repeat(diagonal.size) { offset ->
          val posX: Int = diagonal.x + offset
          val posY: Int = diagonal.y + offset
          val theSame: Boolean = mCallback.areContentsTheSame(posX, posY)
          val changeFlag: Int = if (theSame) FLAG_NOT_CHANGED else FLAG_CHANGED
          mOldItemStatuses[posX] = (posY shl FLAG_OFFSET) or changeFlag
          mNewItemStatuses[posY] = (posX shl FLAG_OFFSET) or changeFlag
        }
      }
      // now all matches are marked, lets look for moves
      if (mDetectMoves) {
        // traverse each addition / removal from the end of the list, find matching
        // addition removal from before
        findMoveMatches()
      }
    }

    private fun findMoveMatches() {
      // for each removal, find matching addition
      var posX = 0
      for (diagonal in mDiagonals) {
        while (posX < diagonal.x) {
          if (mOldItemStatuses[posX] == 0) {
            // there is a removal, find matching addition from the rest
            findMatchingAddition(posX)
          }
          posX++
        }
        // snap back for the next diagonal
        posX = diagonal.endX()
      }
    }

    /**
     * Search the whole list to find the addition for the given removal of position posX
     *
     * @param posX position in the old list
     */
    private fun findMatchingAddition(posX: Int) {
      var posY: Int = 0
      val diagonalsSize: Int = mDiagonals.size
      repeat(diagonalsSize) { i ->
        val diagonal: Diagonal = mDiagonals.get(i)
        while (posY < diagonal.y) {
          // found some additions, evaluate
          if (mNewItemStatuses[posY] == 0) { // not evaluated yet
            val matching: Boolean = mCallback.areItemsTheSame(posX, posY)
            if (matching) {
              // yay found it, set values
              val contentsMatching: Boolean = mCallback.areContentsTheSame(posX, posY)
              val changeFlag: Int = if (contentsMatching) FLAG_MOVED_NOT_CHANGED else FLAG_MOVED_CHANGED
              // once we process one of these, it will mark the other one as ignored.
              mOldItemStatuses[posX] = (posY shl FLAG_OFFSET) or changeFlag
              mNewItemStatuses[posY] = (posX shl FLAG_OFFSET) or changeFlag
              return
            }
          }
          posY++
        }
        posY = diagonal.endY()
      }
    }

    /**
     * Given a position in the old list, returns the position in the new list, or
     * {@code NO_POSITION} if it was removed.
     *
     * @param oldListPosition Position of item in old list
     * @return Position of item in new list, or {@code NO_POSITION} if not present.
     * @see #NO_POSITION
     * @see #convertNewPositionToOld(int)
     */
    fun convertOldPositionToNew(oldListPosition: Int): Int {
      if (oldListPosition < 0 || oldListPosition >= mOldListSize) {
        throw IndexOutOfBoundsException(
          "Index out of bounds - passed position = " +
            oldListPosition + ", old list size = " + mOldListSize,
        )
      }
      val status: Int = mOldItemStatuses[oldListPosition]
      if ((status and FLAG_MASK) == 0) {
        return NO_POSITION
      } else {
        return status shr FLAG_OFFSET
      }
    }

    /**
     * Given a position in the new list, returns the position in the old list, or
     * {@code NO_POSITION} if it was removed.
     *
     * @param newListPosition Position of item in new list
     * @return Position of item in old list, or {@code NO_POSITION} if not present.
     * @see #NO_POSITION
     * @see #convertOldPositionToNew(int)
     */
    fun convertNewPositionToOld(newListPosition: Int): Int {
      if (newListPosition < 0 || newListPosition >= mNewListSize) {
        throw IndexOutOfBoundsException(
          "Index out of bounds - passed position = " +
            newListPosition + ", new list size = " + mNewListSize,
        )
      }
      val status: Int = mNewItemStatuses[newListPosition]
      if ((status and FLAG_MASK) == 0) {
        return NO_POSITION
      } else {
        return status shr FLAG_OFFSET
      }
    }

    /**
     * Dispatches update operations to the given Callback.
     * <p>
     * These updates are atomic such that the first update call affects every update call that
     * comes after it (the same as RecyclerView).
     *
     * @param updateCallback The callback to receive the update operations.
     * @see #dispatchUpdatesTo(RecyclerView.Adapter)
     */
    fun dispatchUpdatesTo(updateCallback: ListUpdateCallback) {
      var updateCallback = updateCallback
      val batchingCallback: BatchingListUpdateCallback

      if (updateCallback is BatchingListUpdateCallback) {
        batchingCallback = updateCallback
      } else {
        batchingCallback = BatchingListUpdateCallback(updateCallback)
        // replace updateCallback with a batching callback and override references to
        // updateCallback so that we don't call it directly by mistake
        @Suppress("UNUSED_VARIABLE")
        updateCallback = batchingCallback
      }
      // track up to date current list size for moves
      // when a move is found, we record its position from the end of the list (which is
      // less likely to change since we iterate in reverse).
      // Later when we find the match of that move, we dispatch the update
      var currentListSize: Int = mOldListSize
      // list of postponed moves
      val postponedUpdates: MutableCollection<PostponedUpdate> = ArrayDeque<PostponedUpdate>()
      // posX and posY are exclusive
      var posX: Int = mOldListSize
      var posY: Int = mNewListSize
      // iterate from end of the list to the beginning.
      // this just makes offsets easier since changes in the earlier indices has an effect
      // on the later indices.
      var diagonalIndex: Int = mDiagonals.size - 1
      while (diagonalIndex >= 0) {
        val diagonal: Diagonal = mDiagonals.get(diagonalIndex)
        val endX: Int = diagonal.endX()
        val endY: Int = diagonal.endY()
        // dispatch removals and additions until we reach to that diagonal
        // first remove then add so that it can go into its place and we don't need
        // to offset values
        while (posX > endX) {
          posX--
          // REMOVAL
          val status: Int = mOldItemStatuses[posX]
          if ((status and FLAG_MOVED) != 0) {
            val newPos: Int = status shr FLAG_OFFSET
            // get postponed addition
            val postponedUpdate: PostponedUpdate? = getPostponedUpdate(postponedUpdates, newPos, false)
            if (postponedUpdate != null) {
              // this is an addition that was postponed. Now dispatch it.
              val updatedNewPos: Int = currentListSize - postponedUpdate.currentPos
              batchingCallback.onMoved(posX, updatedNewPos - 1)
              if ((status and FLAG_MOVED_CHANGED) != 0) {
                val changePayload: Any? = mCallback.getChangePayload(posX, newPos)
                batchingCallback.onChanged(updatedNewPos - 1, 1, changePayload)
              }
            } else {
              // first time we are seeing this, we'll see a matching addition
              postponedUpdates.add(
                PostponedUpdate(
                  posX,
                  currentListSize - posX - 1,
                  true,
                ),
              )
            }
          } else {
            // simple removal
            batchingCallback.onRemoved(posX, 1)
            currentListSize--
          }
        }
        while (posY > endY) {
          posY--
          // ADDITION
          val status: Int = mNewItemStatuses[posY]
          if ((status and FLAG_MOVED) != 0) {
            // this is a move not an addition.
            // see if this is postponed
            val oldPos: Int = status shr FLAG_OFFSET
            // get postponed removal
            val postponedUpdate: PostponedUpdate? = getPostponedUpdate(
              postponedUpdates,
              oldPos,
              true,
            )
            // empty size returns 0 for indexOf
            if (postponedUpdate == null) {
              // postpone it until we see the removal
              postponedUpdates.add(
                PostponedUpdate(
                  posY,
                  currentListSize - posX,
                  false,
                ),
              )
            } else {
              // oldPosFromEnd = foundListSize - posX
              // we can find posX if we swap the list sizes
              // posX = listSize - oldPosFromEnd
              val updatedOldPos: Int = currentListSize - postponedUpdate.currentPos - 1
              batchingCallback.onMoved(updatedOldPos, posX)
              if ((status and FLAG_MOVED_CHANGED) != 0) {
                val changePayload: Any? = mCallback.getChangePayload(oldPos, posY)
                batchingCallback.onChanged(posX, 1, changePayload)
              }
            }
          } else {
            // simple addition
            batchingCallback.onInserted(posX, 1)
            currentListSize++
          }
        }
        // now dispatch updates for the diagonal
        posX = diagonal.x
        posY = diagonal.y
        repeat(diagonal.size) { _ ->
          // dispatch changes
          if ((mOldItemStatuses[posX] and FLAG_MASK) == FLAG_CHANGED) {
            val changePayload: Any? = mCallback.getChangePayload(posX, posY)
            batchingCallback.onChanged(posX, 1, changePayload)
          }
          posX++
          posY++
        }
        // snap back for the next diagonal
        posX = diagonal.x
        posY = diagonal.y
        diagonalIndex--
      }
      batchingCallback.dispatchLastEvent()
    }

    private fun getPostponedUpdate(
      postponedUpdates: MutableCollection<PostponedUpdate>,
      posInList: Int,
      removal: Boolean,
    ): PostponedUpdate? {
      var postponedUpdate: PostponedUpdate? = null
      val itr: MutableIterator<PostponedUpdate> = postponedUpdates.iterator()
      while (itr.hasNext()) {
        val update: PostponedUpdate = itr.next()
        if (update.posInOwnerList == posInList && update.removal == removal) {
          postponedUpdate = update
          itr.remove()
          break
        }
      }
      while (itr.hasNext()) {
        // re-offset all others
        val update: PostponedUpdate = itr.next()
        if (removal) {
          update.currentPos--
        } else {
          update.currentPos++
        }
      }
      return postponedUpdate
    }
  }

  /**
   * Represents an update that we skipped because it was a move.
   * <p>
   * When an update is skipped, it is tracked as other updates are dispatched until the matching
   * add/remove operation is found at which point the tracked position is used to dispatch the
   * update.
   */
  private class PostponedUpdate(
    /**
     * position in the list that owns this item
     */
    val posInOwnerList: Int,

    /**
     * position wrt to the end of the list
     */
    var currentPos: Int,

    /**
     * true if this is a removal, false otherwise
     */
    val removal: Boolean,
  )

  /**
   * Array wrapper w/ negative index support.
   * We use this array instead of a regular array so that algorithm is easier to read without
   * too many offsets when accessing the "k" array in the algorithm.
   */
  class CenteredArray(size: Int) {
    private val mData = IntArray(size)
    private val mMid: Int = mData.size / 2

    fun get(index: Int): Int {
      return mData[index + mMid]
    }

    fun backingData(): IntArray {
      return mData
    }

    fun set(index: Int, value: Int) {
      mData[index + mMid] = value
    }

    fun fill(value: Int) {
      mData.fill(value)
    }
  }
}
