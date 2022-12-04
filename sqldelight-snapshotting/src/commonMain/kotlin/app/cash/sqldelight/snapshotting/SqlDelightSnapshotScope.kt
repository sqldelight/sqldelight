package app.cash.sqldelight.snapshotting

import app.cash.sqldelight.Query
import com.jakewharton.picnic.Table
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer

class SqlDelightSnapshotScope internal constructor(
  @PublishedApi internal val serializersModule: SerializersModule,
  @PublishedApi internal val onRecord: (Table) -> Unit,
) {
  inline fun <reified T : Any> Query<T>.record() {
    val table = Picnic(serializersModule).encode(serializersModule.serializer(), executeAsList())

    // TODO: do something with the table, just print it for now
    println(table)
    onRecord(table)
  }
}
