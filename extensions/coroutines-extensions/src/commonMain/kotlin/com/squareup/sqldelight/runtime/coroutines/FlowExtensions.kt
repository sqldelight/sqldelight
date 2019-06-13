/*
 * Copyright (C) 2018 Square, Inc.
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

@file:JvmName("FlowQuery")

package com.squareup.sqldelight.runtime.coroutines

import com.squareup.sqldelight.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads

/** Turns this [Query] into a [Flow] which emits whenever the underlying result set changes. */
@ExperimentalCoroutinesApi
@JvmName("toFlow")
fun <T : Any> Query<T>.asFlow(): Flow<Query<T>> = callbackFlow<Query<T>> {
  val listener = object : Query.Listener {
    override fun queryResultsChanged() {
      offer(this@asFlow)
    }
  }
  addListener(listener)
  offer(this@asFlow)
  awaitClose {
    removeListener(listener)
  }
}.conflate()

@ExperimentalCoroutinesApi
@JvmOverloads
fun <T : Any> Flow<Query<T>>.mapToOne(
  context: CoroutineContext = Dispatchers.Default
): Flow<T> = map {
  withContext(context) {
    it.executeAsOne()
  }
}

@ExperimentalCoroutinesApi
@JvmOverloads
fun <T : Any> Flow<Query<T>>.mapToOneOrDefault(
  defaultValue: T,
  context: CoroutineContext = Dispatchers.Default
): Flow<T> = map {
  withContext(context) { it.executeAsOneOrNull() } ?: defaultValue
}

@ExperimentalCoroutinesApi
@JvmOverloads
fun <T : Any> Flow<Query<T>>.mapToOneOrNull(
  context: CoroutineContext = Dispatchers.Default
): Flow<T?> = map { withContext(context) { it.executeAsOneOrNull() } }

@ExperimentalCoroutinesApi
@JvmOverloads
fun <T : Any> Flow<Query<T>>.mapToOneNotNull(
  context: CoroutineContext = Dispatchers.Default
): Flow<T> = mapNotNull { withContext(context) { it.executeAsOneOrNull() } }

@ExperimentalCoroutinesApi
@JvmOverloads
fun <T : Any> Flow<Query<T>>.mapToList(
  context: CoroutineContext = Dispatchers.Default
): Flow<List<T>> = map { withContext(context) { it.executeAsList() } }
