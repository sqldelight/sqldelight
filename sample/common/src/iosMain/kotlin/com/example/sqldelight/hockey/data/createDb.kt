package com.example.sqldelight.hockey.data

import com.example.sqldelight.hockey.HockeyDb
import com.squareup.sqldelight.drivers.ios.NativeSqliteDriver

actual fun createDb(): HockeyDb =
    createQueryWrapper(NativeSqliteDriver(Schema, "sampledb"))