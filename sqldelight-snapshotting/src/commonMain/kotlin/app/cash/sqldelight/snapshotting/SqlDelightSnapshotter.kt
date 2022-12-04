package app.cash.sqldelight.snapshotting

import app.cash.sqldelight.snapshotting.utils.writeSnapshot
import com.jakewharton.picnic.Table
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.SerializersModuleBuilder

class SqlDelightSnapshotter(
  private val serializersModule: SerializersModule,
  private val extraSerializers: SerializersModuleBuilder.() -> Unit = {},
) {
  // TODO: Handle "record" vs "verify" modes

  fun snapshot(block: SqlDelightSnapshotScope.() -> Unit) {
    val tables = mutableListOf<Table>()

    SqlDelightSnapshotScope(serializersModule = SerializersModule {
      serializersModule.dumpTo(this)
      extraSerializers()
    }, onRecord = { table -> tables += table }).block()

    // TODO: Don't use a hardcoded path
    writeSnapshot("src/jvmTest/snapshots", tables.joinToString(separator = "\n\n") { it.toString() })
  }
}