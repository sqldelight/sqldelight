# Null Padded Diffing
When placeholders are involved in the PagingSource, sending the diff to RecyclerView is not
trivial and requires a heuristic. This is because the list may be arbitrarily large, and it isn't
possible to resolve item identity from position, as null placeholders in the list may have resolved
into loaded items.

This document explains the algorithm and why it works that way.
Notice that, due to lack of information on the paging side (even the PagingSource), it is
impossible to create a solution that works for all cases. Instead, we do a best effort to cover
as many common cases as we can.

## Basic Algorithm
The original algorithm, implemented [here](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:paging/runtime/src/main/java/androidx/paging/NullPaddedListDiffHelper.kt;drc=63c2310560810fd1ea3a7a90d15493d414ca090f)
does the following:

Given old list with `placeholders before: B0, items: I0, placeholders after:A0`
and new list with `placeholders before: B1, items: I1, placeholders after:A1`
It diffs `I0` and `I1` then adds/removes null items based on the difference between `B0` `B1` and
`A0` `A1`.

This has a couple of problems:
* We don't send any change notifications for existing placeholders. As a result, if developer uses
the position in the binding, it won't be rebound even though placeholder's position changed.
* If a placeholder in the old list is now loaded, we don't send a `change` notification for it
, instead, it either looks like the placehoder moved (if there are still enough placeholders after
) or it got removed.
* When the list size is the same (or similar) but a new disjoint range is loaded, RecyclerView
 shifts.
 For instance, if you have 100 items where only the range 20-30 is loaded,
 If range 40-50 is loaded in the next one, hence:
 `B0` = 20, `I0` = 10, `A0` = 70
 `B1` = 40, `I0` = 10, `A0` = 50

We dispatch `add 20 items to pos 0` and `remove 20 items from pos 80`. If the RecyclerView was
showing placeholders `40-50`, it now thinks they are in positions `60-70` so scrolls to that
position causing the effect of constantly moving towards the end of the list.

Note that this is not necessarily **wrong**. For instance, maybe the last 20 items moved to the
beginning of the list hence it could've been the right dispatch. This is impossible to know
without actually loading placeholders which defeats the purpose of placeholders.
The issue in the current implementation is that it does not optimize for the common case.

## Goals of the new algorithm
* Try to detect cases where we can match new loaded items to placeholders from the previous list
 and send change notifications for them. Similarly, try to convert removals to placeholders when
 possible.
* Try to keep positions stable when distinct lists are loaded.
* For placeholders that stay in the old & new list, send change notifications for them if their
 positions changed to handle cases where developer uses bind position in the placeholder View.

### How it works
* Same as the first algorithm, we diff `I0` and `I1`.
* Instead of dispatching updates with offset as the first algorithm does:
  * If two lists do not overlap at all, use a special diffing where: (DistinctListsDiffDispatcher)
    * We'll send change events for placeholders that became items (based on position)
    * We'll send change events for items that became placeholders (based on position)
    * We'll send add/remove for items that get swapped with other items (based on position)
    * Finally, we'll add/remove placeholders **to the end** to fix the list size
      * This heuristic optimizes for non-reverse layouts
    * This will make RecyclerView re-layout from where it is, nicely handling jumps.
  * If two lists overlap, we'll try to dispatch some of the insert/remove events from the I0-I1
   diff as placeholder **change** events, when possible. This is specifically to cover cases
   where placeholders are loaded as items in the updated list OR items in the old list are
   unloaded (as placeholders).
    * If addition/removal happened within the first and last item (so not the start or end of
     loaded items) dispatch the event as is (with offset for placeholders).
    * For other changes that happen at the edges of the list:
      * For removals, if we will need more placeholders in that direction, dispatch **CHANGE** to
       turn them into placeholders. If we have more removals then new placeholders needed, dispatch
        **REMOVE** for them.
      * For additions, if we have placeholders in that direction, dispatch **CHANGE** for those
       placeholders to turn them into items. If we have more new items than existing placeholders
       , dispatch **INSERT**.
      * When we use placeholders in one edge to handle insert OR remove; we flag that edge as
       "insertion" or "removal". When an edge is flagged as "insertion", it can only be used for
       "insert" events later on. Similarly, if it is flagged as "removal", it can only be used
        for "remove" again. This is to ensure that we never convert an item into placeholder
        then back to an item (or vice versa). Doing that would mean sending two **CHANGE** events
        for an item that will make RecyclerView think that item has been update even though it is
        actually being swapped with a completely different item.
    * Finally, dispatch add/remove events to match the placeholder count. We will insert at index
     0 for leading placeholders and index(size) for trailing placeholders.
    * For event placeholders that stayed in the list, dispatch a change if their positions changed.
      * This technically mean we consider placeholder's position as part of its data. These
       change notifications will direct RecyclerView to rebind them; handling cases where
       developer uses binding position in the placeholder view.