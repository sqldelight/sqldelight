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

package app.cash.sqldelight.coroutines

import app.cash.sqldelight.Query
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmName
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext

/** Turns this [Query] into a [Flow] which emits whenever the underlying result set changes. */
@JvmName("toFlow")
fun <T : Any> Query<T>.asFlow(): Flow<Query<T>> = flow {
  val channel = Channel<Unit>(CONFLATED)
  channel.trySend(Unit)

  val listener = Query.Listener {
    channel.trySend(Unit)
  }

  addListener(listener)
  try {
    for (item in channel) {
      emit(this@asFlow)
    }
  } finally {
    removeListener(listener)
  }
}

fun <T : Any> Flow<Query<T>>.mapToOne(
  context: CoroutineContext,
): Flow<T> = map {
  withContext(context) {
    it.awaitAsOne()
  }
}

fun <T : Any> Flow<Query<T>>.mapToOneOrDefault(
  defaultValue: T,
  context: CoroutineContext,
): Flow<T> = map {
  withContext(context) {
    it.awaitAsOneOrNull() ?: defaultValue
  }
}

fun <T : Any> Flow<Query<T>>.mapToOneOrNull(
  context: CoroutineContext,
): Flow<T?> = map {
  withContext(context) {
    it.awaitAsOneOrNull()
  }
}

fun <T : Any> Flow<Query<T>>.mapToOneNotNull(
  context: CoroutineContext,
): Flow<T> = mapNotNull {
  withContext(context) {
    it.awaitAsOneOrNull()
  }
}

fun <T : Any> Flow<Query<T>>.mapToList(
  context: CoroutineContext,
): Flow<List<T>> = map {
  withContext(context) {
    it.awaitAsList()
  }
}
