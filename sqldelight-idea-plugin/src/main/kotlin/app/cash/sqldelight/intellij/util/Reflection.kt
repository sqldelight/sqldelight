package app.cash.sqldelight.intellij.util

import java.lang.reflect.InvocationTargetException

/**
 * Reflective calls wrap anything thrown by the target in [InvocationTargetException], which hides
 * exceptions such as [com.intellij.openapi.progress.ProcessCanceledException] from callers that
 * need to see them. Rethrow the original exception so it propagates as if the call were direct.
 */
internal inline fun <T> unwrappingInvocationTarget(block: () -> T): T {
  try {
    return block()
  } catch (e: InvocationTargetException) {
    throw e.targetException ?: e
  }
}
