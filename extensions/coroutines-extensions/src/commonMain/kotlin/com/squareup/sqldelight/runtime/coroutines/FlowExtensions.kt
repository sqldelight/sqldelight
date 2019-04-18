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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads

/**
 * Turns this [Query] into a [Flow] which emits whenever the underlying result set changes.
 *
 * @param context By default, emissions occur on the [Dispatchers.Default] context but can be
 * optionally overridden.
 */
@FlowPreview
@JvmOverloads
@JvmName("toFlow")
fun <T : Any> Query<T>.asFlow(
  context: CoroutineContext = Dispatchers.Default
): Flow<Query<T>> = flow {
  val channel = Channel<Unit>(CONFLATED)
  val listener = object : Query.Listener {
    override fun queryResultsChanged() {
      channel.offer(Unit)
    }
  }
  addListener(listener)
  try {
    emit(this@asFlow)
    for (item in channel) {
      emit(this@asFlow)
    }
  } finally {
    removeListener(listener)
  }
}.flowOn(context)

@FlowPreview
fun <T : Any> Flow<Query<T>>.mapToOne(): Flow<T> {
  return map { it.executeAsOne() }
}

@FlowPreview
fun <T : Any> Flow<Query<T>>.mapToOneOrDefault(defaultValue: T): Flow<T> {
  return map { it.executeAsOneOrNull() ?: defaultValue }
}

@FlowPreview
fun <T : Any> Flow<Query<T>>.mapToOneOrNull(): Flow<T?> {
  return map { it.executeAsOneOrNull() }
}

@FlowPreview
fun <T: Any> Flow<Query<T>>.mapToList(): Flow<List<T>> {
  return map { it.executeAsList() }
}

@FlowPreview
fun <T : Any> Flow<Query<T>>.mapToOneNonNull(): Flow<T> {
  return mapNotNull { it.executeAsOneOrNull() }
}
