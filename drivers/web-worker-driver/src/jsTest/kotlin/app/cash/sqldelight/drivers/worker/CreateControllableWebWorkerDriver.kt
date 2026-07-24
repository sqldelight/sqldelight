package app.cash.sqldelight.drivers.worker

import app.cash.sqldelight.driver.worker.SqliteWasmWebWorkerDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import app.cash.sqldelight.driver.worker.WorkerWrapper
import org.w3c.dom.Worker

@Suppress("UnsafeCastFromDynamic")
actual fun createControllableWebWorkerDriver(): SqliteWasmWebWorkerDriver {
  val wrapper = WorkerWrapper(
    Worker(js("""new URL("./controllable.worker.js", import.meta.url)""")),
  )
  return SqliteWasmWebWorkerDriver(
    driver = WebWorkerDriver(wrapper),
    lifecycleWrapper = wrapper,
  )
}
