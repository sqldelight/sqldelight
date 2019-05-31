package com.squareup.sqldelight.core.compiler

class QueryIdGenerator(private val databaseName: String) {
    private var counter: Long = 0

    fun getId(): String {
        return "$databaseName${counter++}"
    }
}
