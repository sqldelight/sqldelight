package com.alecstrong.sqlite.android

class SqlitePluginException(internal val originatingElement: Any, message: String) : IllegalStateException(
    message)
