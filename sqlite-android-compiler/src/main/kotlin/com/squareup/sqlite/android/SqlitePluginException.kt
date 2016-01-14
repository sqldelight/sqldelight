package com.squareup.sqlite.android

class SqlitePluginException(internal val originatingElement: Any, message: String) : IllegalStateException(
    message)
