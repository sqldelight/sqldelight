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
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.MessageEvent
import org.w3c.dom.events.Event

internal actual class WorkerWrapper actual constructor(
  private val worker: Worker,
) {
  actual suspend fun execute(
    request: WorkerWrapperRequest,
  ): WorkerResultWithRowCount = suspendCancellableCoroutine { continuation ->
    var messageListener: ((Event) -> Unit)? = null
    messageListener = { event: Event ->
      val message = event.unsafeCast<MessageEvent>()
      val data = message.data?.unsafeCast<WasmWorkerResponse>()
      if (data == null) {
        continuation.resumeWithException(WebWorkerException("Message ${message.type} data was null or not a WorkerResponse"))
      } else {
        if (data.id == request.id) {
          worker.removeEventListener("message", messageListener)
          if (data.error != null) {
            continuation.resumeWithException(
              WebWorkerException(
                jsonStringify(
                  value = data.error,
                  replacer = listOf(
                    "message",
                    "arguments",
                    "type",
                    "name",
                  ).toJsArray { it.toJsString() },
                ),
              ),
            )
          } else {
            continuation.resume(
              WasmWorkerResultWithRowCount(data),
            )
          }
        }
      }
    }
    var errorListener: ((Event) -> Unit)? = null
    errorListener = { event ->
      worker.removeEventListener("error", errorListener)
      continuation.resumeWithException(
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
    worker.addEventListener("message", messageListener)
    worker.addEventListener("error", errorListener)

    val messageObject = instantiateObject<WasmWorkerRequest>().apply {
      this.id = request.id
      this.action = request.action
      this.sql = request.sql
      this.params = request.statement?.parameters
    }

    worker.postMessage(messageObject)

    continuation.invokeOnCancellation {
      worker.removeEventListener("message", messageListener)
      worker.removeEventListener("error", errorListener)
    }
  }

  actual fun terminate() {
    worker.terminate()
  }

}
