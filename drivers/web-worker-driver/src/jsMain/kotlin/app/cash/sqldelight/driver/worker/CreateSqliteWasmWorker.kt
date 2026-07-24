package app.cash.sqldelight.driver.worker

import app.cash.sqldelight.driver.worker.expected.Worker

@Suppress("UnsafeCastFromDynamic")
internal actual fun createSqliteWasmWorker(): Worker = js("""new Worker(new URL("@cashapp/sqldelight-sqlite-wasm-worker/sqlite-wasm.worker.js", import.meta.url), { type: "module" })""")
