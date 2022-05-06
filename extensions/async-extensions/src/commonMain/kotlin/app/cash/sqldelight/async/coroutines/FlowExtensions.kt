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

package app.cash.sqldelight.async.coroutines

import app.cash.sqldelight.async.AsyncQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads

/** Turns this [AsyncQuery] into a [Flow] which emits whenever the underlying result set changes. */
@JvmName("toFlow")
fun <T : Any> AsyncQuery<T>.asFlow(): Flow<AsyncQuery<T>> = flow {
  val channel = Channel<Unit>(CONFLATED)
  channel.trySend(Unit)

  val listener = object : AsyncQuery.Listener {
    override fun queryResultsChanged() {
      channel.trySend(Unit)
    }
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

@JvmOverloads
fun <T : Any> Flow<AsyncQuery<T>>.mapToOne(
  context: CoroutineContext = Dispatchers.Default
): Flow<T> = map {
  withContext(context) {
    it.executeAsOne()
  }
}

@JvmOverloads
fun <T : Any> Flow<AsyncQuery<T>>.mapToOneOrDefault(
  defaultValue: T,
  context: CoroutineContext = Dispatchers.Default
): Flow<T> = map {
  withContext(context) {
    it.executeAsOneOrNull() ?: defaultValue
  }
}

@JvmOverloads
fun <T : Any> Flow<AsyncQuery<T>>.mapToOneOrNull(
  context: CoroutineContext = Dispatchers.Default
): Flow<T?> = map {
  withContext(context) {
    it.executeAsOneOrNull()
  }
}

@JvmOverloads
fun <T : Any> Flow<AsyncQuery<T>>.mapToOneNotNull(
  context: CoroutineContext = Dispatchers.Default
): Flow<T> = mapNotNull {
  withContext(context) {
    it.executeAsOneOrNull()
  }
}

@JvmOverloads
fun <T : Any> Flow<AsyncQuery<T>>.mapToList(
  context: CoroutineContext = Dispatchers.Default
): Flow<List<T>> = map {
  withContext(context) {
    it.executeAsList()
  }
}
