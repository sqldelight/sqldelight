package app.cash.sqldelight.snapshotting

import com.jakewharton.picnic.Table
import com.jakewharton.picnic.table
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
public class Picnic(override val serializersModule: SerializersModule = EmptySerializersModule()) : AbstractEncoder() {
  private val header = mutableListOf<String>()
  private val rows = mutableListOf<MutableList<String>>()

  override fun encodeValue(value: Any) {
    rows.last() += value.toString()
  }

  fun <T> encode(serializer: SerializationStrategy<T>, value: List<T>): Table {
    header += serializer.descriptor.elementNames
    value.forEach {
      rows += mutableListOf<String>()
      encodeSerializableValue(serializer, it)
    }

    return table {
      cellStyle { border = true }
      header {
        row {
          cell(serializer.descriptor.serialName) {
            border = false
            columnSpan = header.size
          }
        }
        row {
          header.forEach { cell(it) }
        }
      }
      rows.forEach { items ->
        row { items.forEach { cell(it) } }
      }
    }
  }
}
