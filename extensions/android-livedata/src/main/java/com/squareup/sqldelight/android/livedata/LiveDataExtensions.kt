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

@file:JvmName("LiveDataQuery")

package com.squareup.sqldelight.android.livedata

import android.annotation.SuppressLint
import androidx.annotation.CheckResult
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.squareup.sqldelight.Query
import java.util.concurrent.Executor

@JvmName("toLiveData")
@CheckResult
fun <T : Any> Query<T>.asLiveData(): LiveData<Query<T>> =
  object : LiveData<Query<T>>(), Query.Listener {

    override fun queryResultsChanged() {
      postValue(this@asLiveData)
    }

    override fun onActive() {
      addListener(this)
      postValue(this@asLiveData)
    }

    override fun onInactive() {
      removeListener(this)
    }
  }

private val ioExecutor: Executor by lazy {
  @SuppressLint("RestrictedApi")
  val ioExecutor = ArchTaskExecutor.getIOThreadExecutor()
  ioExecutor
}

@CheckResult
@JvmOverloads
fun <T : Any> LiveData<Query<T>>.mapToOne(
  executor: Executor = ioExecutor
): LiveData<T> {
  val result = MediatorLiveData<T>()
  result.addSource(this) { query ->
    executor.execute {
      query.execute().use {
        if (it.next()) {
          result.postValue(query.mapper(it))
        }
      }
    }
  }
  return result
}

@CheckResult
@JvmOverloads
fun <T : Any> LiveData<Query<T>>.mapToOneOrNull(
  executor: Executor = ioExecutor
): LiveData<T> {
  val result = MediatorLiveData<T>()
  result.addSource(this) { query ->
    executor.execute {
      query.execute().use {
        result.postValue(if (it.next()) query.mapper(it) else null)
      }
    }
  }
  return result
}

@CheckResult
@JvmOverloads
fun <T : Any> LiveData<Query<T>>.mapToList(
  executor: Executor = ioExecutor
): LiveData<List<T>> {
  val result = MediatorLiveData<List<T>>()
  result.addSource(this) { query ->
    executor.execute { result.postValue(query.executeAsList()) }
  }
  return result
}
