package com.example.sqldelight.hockey.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker

actual fun webWorkerDriver(): SqlDriver =
  WebWorkerDriver(
    createWorker(),
  )

fun createWorker(): Worker =
  js("""new Worker(new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url))""")
