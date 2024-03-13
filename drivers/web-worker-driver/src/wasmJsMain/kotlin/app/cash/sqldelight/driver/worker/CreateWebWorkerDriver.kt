package app.cash.sqldelight.driver.worker

import app.cash.sqldelight.db.SqlDriver
import org.w3c.dom.Worker

actual fun createWebWorkerDriver(): SqlDriver {
  return WebWorkerDriver(jsWorker())
}

internal fun jsWorker(): Worker =
  js("""new Worker(new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url))""")
