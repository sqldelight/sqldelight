package com.squareup.sqldelight.drivers.ios.util

import co.touchlab.stately.collections.SharedHashMap
import co.touchlab.stately.collections.SharedLinkedList

/**
 * This should probably be in Stately
 */
internal fun <T> SharedLinkedList<T>.cleanUp(block: (T) -> Unit) {
  val extractList = ArrayList<T>(size)
  extractList.addAll(this)
  this.clear()
  extractList.forEach(block)
}

internal fun <K, V> SharedHashMap<K, V>.cleanUp(block: (Map.Entry<K, V>) -> Unit) {
  val extractMap = HashMap<K, V>(this.size)
  extractMap.putAll(this)
  this.clear()
  extractMap.forEach(block)
}