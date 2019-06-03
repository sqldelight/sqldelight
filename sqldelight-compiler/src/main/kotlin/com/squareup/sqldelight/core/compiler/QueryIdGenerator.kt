package com.squareup.sqldelight.core.compiler

import java.util.concurrent.atomic.AtomicInteger

/**
 * When generating IDs for queries, we need to ensure they are done in a deterministic way
 * The ordering of database generation is non-deterministic so we scope the ids to a database
 * Secondly, we need to ensure the counter is reset between gradle invocations.
 * This class should be instantiated at the beginning of code generation
 */
class QueryIdGenerator(private val databaseName: String) {
    private var counter: AtomicInteger = AtomicInteger(0)

    val nextId: Int
        get() {
            return "$databaseName${counter.getAndIncrement()}".hashCode()
        }
}
