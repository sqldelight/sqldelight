package app.cash.sqldelight.internal

import kotlin.native.concurrent.Worker

internal actual fun currentThreadId(): Long = Worker.current.id.toLong()
