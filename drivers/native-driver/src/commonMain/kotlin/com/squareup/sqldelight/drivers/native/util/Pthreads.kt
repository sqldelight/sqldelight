package com.squareup.sqldelight.drivers.native.util

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect class PthreadMutexPointed

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect class PthreadConditionPointed

@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect fun createPthreadMutex(): PthreadMutexPointed

@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect fun createPthreadCondition(): PthreadConditionPointed

@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect fun PthreadMutexPointed.lock()

@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect fun PthreadMutexPointed.unlock()

@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect fun PthreadConditionPointed.wait(mutex: PthreadMutexPointed)

@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect fun PthreadConditionPointed.signal()

@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect fun PthreadMutexPointed.destroy()

@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect fun PthreadConditionPointed.destroy()
