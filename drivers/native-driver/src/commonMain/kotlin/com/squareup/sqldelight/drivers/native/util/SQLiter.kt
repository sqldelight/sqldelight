package com.squareup.sqldelight.drivers.native.util

import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.DatabaseManager

@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect fun createDatabaseManager(configuration: DatabaseConfiguration): DatabaseManager
