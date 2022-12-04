package app.cash.sqldelight.snapshotting

import app.cash.sqldelight.Query
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

typealias DumpMapper = String.() -> Query<*>

class SqlDelightDumper(val builder: DumperBuilderScope.() -> Unit) {

    val queries = DumperBuilderScope().apply { builder() }.queries

    fun dump() {
        TODO()
    }
}

class DumperBuilderScope internal constructor() {
    val queries: MutableList<Pair<String, Query<*>>> = mutableListOf()
    operator fun String.invoke(query: () -> Query<*>): Pair<String, Query<*>> = this to query()
}

fun eg() {
//    SqlDelightDumper(
//        "This is my table" { TODO() }
//    )

    String::class.simpleName

    String.serializer()

    Json.encodeToString("Hello World")
}
