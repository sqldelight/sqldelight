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
package com.squareup.sqldelight.util

import java.util.ArrayList
import java.util.LinkedHashMap

class BiMultiMap<K, V> : LinkedHashMap<K, List<V>> {
  private val reversed: LinkedHashMap<V, K>

  constructor() : super() {
    reversed = linkedMapOf()
  }

  constructor(vararg pairs: Pair<K, List<V>>) : super(pairs.toMap()) {
    reversed = LinkedHashMap(pairs.flatMap{ pair -> pair.second.map { Pair(it, pair.first) } }.toMap())
  }

  private constructor(
      otherMap: Map<K, List<V>>,
      otherReversed: Map<V, K>
  ) : super(otherMap) {
    reversed = LinkedHashMap(otherReversed)
  }

  fun put(key: K, value: V) {
    var list = get(key)
    if (list == null) {
      list = ArrayList<V>()
    }
    (list as MutableList).add(value)
    reversed.put(value, key)
  }

  fun getForValue(value: V) = reversed[value]!!

  operator fun plus(other: BiMultiMap<K, V>): BiMultiMap<K, V>
      = BiMultiMap((this as LinkedHashMap<K, List<V>>) + other, reversed + other.reversed)

  operator fun minus(key: K): BiMultiMap<K, V>
      = BiMultiMap(filter { it.key != key }, reversed.filter { it.value != key })
}

internal fun <K, V> emptyBiMultiMap() = BiMultiMap<K, V>()