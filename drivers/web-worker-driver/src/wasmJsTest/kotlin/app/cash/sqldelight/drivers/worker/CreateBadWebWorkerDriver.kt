package app.cash.sqldelight.drivers.worker

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker

actual fun createBadWebWorkerDriver(): SqlDriver {
  return WebWorkerDriver(badJsWorker())
}

fun badJsWorker(): Worker =
  js("""new Worker(new URL("./bad.worker.js", import.meta.url))""")
