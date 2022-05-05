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
package app.cash.sqldelight.async.db

/**
 * A type that can be closed.
 */
interface AsyncCloseable {
  /**
   * Close any resources backed by this object.
   */
  suspend fun close()
}

/**
 * Run [body] on the receiver and call [AsyncCloseable.close] before returning or throwing.
 */
suspend inline fun <T : AsyncCloseable?, R> T.use(body: (T) -> R): R {
  var exception: Throwable? = null
  try {
    return body(this)
  } catch (e: Throwable) {
    exception = e
    throw e
  } finally {
    when {
      this == null -> {}
      exception == null -> close()
      else ->
        try {
          close()
        } catch (closeException: Throwable) {
          // Nothing to do...
        }
    }
  }
}
