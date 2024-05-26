package app.cash.sqldelight.driver.worker

import app.cash.sqldelight.driver.worker.api.JsWorkerResponse
import app.cash.sqldelight.driver.worker.api.WorkerResultWithRowCount
import app.cash.sqldelight.driver.worker.api.WorkerWrapperRequest
import app.cash.sqldelight.driver.worker.api.buildRequest
import app.cash.sqldelight.driver.worker.expected.JsWorkerResultWithRowCount
import app.cash.sqldelight.driver.worker.expected.Worker
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.MessageEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener

internal actual class WorkerWrapper actual constructor(
  private val worker: Worker,
) {
  actual suspend fun execute(
    request: WorkerWrapperRequest,
  ): WorkerResultWithRowCount {
    return suspendCancellableCoroutine { continuation ->
      val messageListener = object : EventListener {
        override fun handleEvent(event: Event) {
          val data = event.unsafeCast<MessageEvent>().data.unsafeCast<JsWorkerResponse>()
          if (data.id == request.id) {
            worker.removeEventListener("message", this)
            if (data.error != null) {
              continuation.resumeWithException(
                WebWorkerException(
                  JSON.stringify(
                    data.error,
                    arrayOf("message", "arguments", "type", "name"),
                  ),
                ),
              )
            } else {
              continuation.resume(
                JsWorkerResultWithRowCount(data),
              )
            }
          }
        }
      }

      val errorListener = object : EventListener {
        override fun handleEvent(event: Event) {
          worker.removeEventListener("error", this)
          continuation.resumeWithException(
            WebWorkerException(
              JSON.stringify(
                event,
                arrayOf("message", "arguments", "type", "name"),
              ) + js("Object.entries(event)"),
            ),
          )
        }
      }

      worker.addEventListener("message", messageListener)
      worker.addEventListener("error", errorListener)

      val messageObject = buildRequest {
        this.id = request.id
        this.action = request.action
        this.sql = request.sql
        this.params = request.statement?.parameters?.toTypedArray()
      }

      worker.postMessage(messageObject)

      continuation.invokeOnCancellation {
        worker.removeEventListener("message", messageListener)
        worker.removeEventListener("error", errorListener)
      }

    }
  }

  actual fun terminate() {
    worker.terminate()
  }
}
