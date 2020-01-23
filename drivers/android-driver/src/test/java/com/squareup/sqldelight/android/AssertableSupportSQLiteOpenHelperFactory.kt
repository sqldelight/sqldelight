package com.squareup.sqldelight.android

import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration
import androidx.sqlite.db.SupportSQLiteOpenHelper.Factory
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory

class AssertableSupportSQLiteOpenHelperFactory(private val delegate: Factory = FrameworkSQLiteOpenHelperFactory()) : Factory {

    lateinit var lastConfiguration: Configuration

    override fun create(configuration: Configuration): SupportSQLiteOpenHelper {
        lastConfiguration = configuration
        return delegate.create(configuration)
    }
}
