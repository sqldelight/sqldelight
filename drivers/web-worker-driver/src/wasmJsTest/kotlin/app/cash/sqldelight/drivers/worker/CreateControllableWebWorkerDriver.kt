package app.cash.sqldelight.drivers.worker

import app.cash.sqldelight.driver.worker.SqliteWasmWebWorkerDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import app.cash.sqldelight.driver.worker.WorkerWrapper
import org.w3c.dom.Worker

actual fun createControllableWebWorkerDriver(): SqliteWasmWebWorkerDriver {
  val wrapper = WorkerWrapper(controllableWorker())
  return SqliteWasmWebWorkerDriver(
    driver = WebWorkerDriver(wrapper),
    lifecycleWrapper = wrapper,
    lifecycleRequestId = -1,
  )
}

private fun controllableWorker(): Worker = js("""new Worker(new URL("./controllable.worker.js", import.meta.url))""")
