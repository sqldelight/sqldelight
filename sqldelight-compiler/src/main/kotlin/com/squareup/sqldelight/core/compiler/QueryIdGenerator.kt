package com.squareup.sqldelight.core.compiler

/**
 * When generating IDs for queries, we need to ensure they are done in a deterministic way
 * The ordering of database generation is non-deterministic so we scope the ids to a database
 * Secondly, we need to ensure the counter is reset between gradle invocations.
 * This class should be instantiated at the beginning of code generation
 *
 * Note: This implementation is not thread-safe and should therefore not be shared between gradle tasks
 */
class QueryIdGenerator(private val databaseName: String) {
    private var counter: Int = 0

    val nextId: Int
        get() {
            return "$databaseName${counter++}".hashCode()
        }
}
