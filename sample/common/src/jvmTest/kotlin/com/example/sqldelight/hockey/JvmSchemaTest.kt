package com.example.sqldelight.hockey

import app.cash.sqldelight.snapshotting.Picnic
import app.cash.sqldelight.snapshotting.SqlDelightSnapshotter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.util.GregorianCalendar
import kotlin.test.Test
import kotlin.test.assertTrue

object GregorianCalendarSerializer : KSerializer<GregorianCalendar> {
  override fun deserialize(decoder: Decoder): GregorianCalendar = throw UnsupportedOperationException()

  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("GregorianCalendar", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: GregorianCalendar) {
    encoder.encodeString(value.toZonedDateTime().toString())
  }
}

class JvmSchemaTest : BaseTest() {

  private val sqlDelightSnapshotter = SqlDelightSnapshotter(HockeyDbSerializersModule, extraSerializers = {
    contextual(GregorianCalendar::class, GregorianCalendarSerializer)
  })

  @Test
  fun teamsCreated() {
    val teams = getDb().teamQueries.selectAll().executeAsList()

    sqlDelightSnapshotter.snapshot {
      getDb().teamQueries.selectAll().record()
    }

    assertTrue(
      teams.any {
        it.name == "Anaheim Ducks"
      },
    )
  }

  @Test
  fun playersCreated() {
    val players = getDb().playerQueries.selectAll().executeAsList()
    assertTrue(
      players.any {
        it.last_name == "Karlsson"
      },
    )
  }
}
