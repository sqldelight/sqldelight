package app.cash.sqldelight.drivers.worker

import app.cash.sqldelight.driver.worker.SqliteWasmWebWorkerDriver

expect fun createControllableWebWorkerDriver(): SqliteWasmWebWorkerDriver
