package app.cash.sqldelight.driver.worker

import app.cash.sqldelight.driver.worker.api.WasmWorkerRequest
import app.cash.sqldelight.driver.worker.api.WasmWorkerResponse
import app.cash.sqldelight.driver.worker.api.WorkerResultWithRowCount
import app.cash.sqldelight.driver.worker.api.WorkerWrapperRequest
import app.cash.sqldelight.driver.worker.expected.Worker
import app.cash.sqldelight.driver.worker.util.instantiateObject
import app.cash.sqldelight.driver.worker.util.jsonStringify
import app.cash.sqldelight.driver.worker.util.objectEntries
import app.cash.sqldelight.driver.worker.util.toJsArray
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.MessageEvent
import org.w3c.dom.events.Event

internal actual class WorkerWrapper actual constructor(
  private val worker: Worker,
) {
  private val pendingRequests = mutableMapOf<Int, CancellableContinuation<WorkerResultWithRowCount>>()
  private var closed = false
  private var closureCause: WebWorkerException? = null

  private val messageListener: (Event) -> Unit = { event ->
    val message = event.unsafeCast<MessageEvent>()
    val data = message.data?.unsafeCast<WasmWorkerResponse>()
    if (data == null) {
      failPending(
        WebWorkerException("Message ${message.type} data was null or not a WorkerResponse"),
      )
    } else if (data.error != null) {
      completeExceptionally(
        data.id,
        WebWorkerException(
          jsonStringify(
            value = data.error,
            replacer = listOf("message", "arguments", "type", "name").toJsArray { it.toJsString() },
          ),
        ),
      )
    } else {
      complete(data.id, WasmWorkerResultWithRowCount(data))
    }
  }

  private val errorListener: (Event) -> Unit = { event ->
    event.preventDefault()
    close(
      WebWorkerException(
        jsonStringify(
          event,
          listOf(
            "message",
            "arguments",
            "type",
            "name",
          ).toJsArray { it.toJsString() },
        ) + objectEntries(event),
      ),
    )
  }

  init {
    worker.addEventListener("message", messageListener)
    worker.addEventListener("error", errorListener)
  }

  actual suspend fun execute(
    request: WorkerWrapperRequest,
  ): WorkerResultWithRowCount {
    return suspendCancellableCoroutine { continuation ->
      if (closed) {
        continuation.resumeWithException(closureCause ?: closedException())
        return@suspendCancellableCoroutine
      }

      if (pendingRequests.containsKey(request.id)) {
        continuation.resumeWithException(
          WebWorkerException("A Web Worker request with id ${request.id} is already pending"),
        )
        return@suspendCancellableCoroutine
      }

      pendingRequests[request.id] = continuation
      continuation.invokeOnCancellation {
        if (pendingRequests[request.id] === continuation) {
          pendingRequests.remove(request.id)
        }
      }

      val messageObject = instantiateObject<WasmWorkerRequest>().apply {
        this.id = request.id
        this.action = request.action
        this.sql = request.sql
        this.params = request.statement?.parameters
        this.databaseName = request.databaseName
      }

      try {
        worker.postMessage(messageObject)
      } catch (throwable: Throwable) {
        completeExceptionally(request.id, throwable)
      }
    }
  }

  actual fun terminate() {
    close(closedException())
  }

  private fun close(cause: WebWorkerException) {
    if (closed) return
    closed = true
    closureCause = cause
    worker.removeEventListener("message", messageListener)
    worker.removeEventListener("error", errorListener)

    try {
      worker.terminate()
    } finally {
      failPending(cause)
    }
  }

  private fun complete(
    id: Int,
    result: WorkerResultWithRowCount,
  ) {
    val continuation = pendingRequests.remove(id) ?: return
    continuation.resume(result)
  }

  private fun completeExceptionally(
    id: Int,
    throwable: Throwable,
  ) {
    val continuation = pendingRequests.remove(id) ?: return
    continuation.resumeWithException(throwable)
  }

  private fun failPending(throwable: Throwable) {
    val continuations = pendingRequests.values.toList()
    pendingRequests.clear()
    continuations.forEach { continuation ->
      continuation.resumeWithException(throwable)
    }
  }

  private fun closedException(): WebWorkerException {
    return WebWorkerException("The Web Worker has been terminated")
  }
}
