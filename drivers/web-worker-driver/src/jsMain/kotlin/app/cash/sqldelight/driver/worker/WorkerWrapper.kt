package app.cash.sqldelight.driver.worker

import app.cash.sqldelight.driver.worker.api.JsWorkerResponse
import app.cash.sqldelight.driver.worker.api.WorkerResultWithRowCount
import app.cash.sqldelight.driver.worker.api.WorkerWrapperRequest
import app.cash.sqldelight.driver.worker.api.buildRequest
import app.cash.sqldelight.driver.worker.expected.JsWorkerResultWithRowCount
import app.cash.sqldelight.driver.worker.expected.Worker
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.MessageEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener

internal actual class WorkerWrapper actual constructor(
  private val worker: Worker,
) {
  private val pendingRequests = mutableMapOf<Int, CancellableContinuation<WorkerResultWithRowCount>>()
  private var nextRequestId = 0
  private var closed = false
  private var closureCause: WebWorkerException? = null

  private val messageListener = object : EventListener {
    override fun handleEvent(event: Event) {
      val data = event.unsafeCast<MessageEvent>().data.unsafeCast<JsWorkerResponse>()
      if (data.error != null) {
        completeExceptionally(
          data.id,
          WebWorkerException(
            JSON.stringify(
              data.error,
              arrayOf("message", "arguments", "type", "name"),
            ),
          ),
        )
      } else {
        complete(data.id, JsWorkerResultWithRowCount(data))
      }
    }
  }

  private val errorListener = object : EventListener {
    override fun handleEvent(event: Event) {
      event.preventDefault()
      close(
        WebWorkerException(
          JSON.stringify(
            event,
            arrayOf("message", "arguments", "type", "name"),
          ) + js("Object.entries(event)"),
        ),
      )
    }
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

      val id = nextRequestId++
      pendingRequests[id] = continuation
      continuation.invokeOnCancellation {
        if (pendingRequests[id] === continuation) {
          pendingRequests.remove(id)
        }
      }

      val messageObject = buildRequest {
        this.id = id
        this.action = request.action
        this.sql = request.sql
        this.params = request.statement?.parameters?.toTypedArray()
        this.databaseName = request.databaseName
      }

      try {
        worker.postMessage(messageObject)
      } catch (throwable: Throwable) {
        completeExceptionally(id, throwable)
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
