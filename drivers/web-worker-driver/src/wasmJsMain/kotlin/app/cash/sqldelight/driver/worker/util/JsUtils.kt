package app.cash.sqldelight.driver.worker.util

import org.khronos.webgl.Uint8Array
import org.khronos.webgl.set

internal fun jsonStringify(value: JsAny?, replacer: JsArray<JsString>? = null, space: String? = null): String =
  js("JSON.stringify(value, replacer, space)")

internal fun objectEntries(value: JsAny?): JsArray<JsArray<JsString>> = js("Object.entries(value)")

internal fun isArray(value: JsAny?): Boolean =
  js("Array.isArray(value)")

internal fun <T : JsAny?> JsArray<T>.add(value: T) {
  jsArrayPush(this, value)
}

@Suppress("UNUSED_PARAMETER")
private fun <T : JsAny?> jsArrayPush(array: JsArray<T>, value: T) {
  js("array.push(value)")
}

internal fun ByteArray.toUint8Array(): Uint8Array = Uint8Array(size).apply {
  forEachIndexed { index, byte -> this[index] = byte }
}

internal fun <T, R : JsAny> Iterable<T>.toJsArray(mapper: (T) -> R): JsArray<R> =
  JsArray<R>().apply {
    forEach { add(mapper(it)) }
  }

internal fun <T : JsAny> instantiateObject(): T =
  js("({})")
