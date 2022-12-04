package com.sample

import java.util.List
import kotlin.Boolean
import kotlin.Long
import kotlin.String
import kotlin.Unit
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

public object PersonSerializer : KSerializer<PersonSerializer> {
  public override val descriptor: SerialDescriptor

  init {
    buildClassSerialDescriptor("person") {
      element<Long>("_id")
      element<String>("name")
      element<String?>("last_name")
      element<Boolean>("is_cool")
      element<List<Person>?>("friends")
      element<String>("shhh_its_secret")
    }
  }

  public override fun serialize(encoder: Encoder, `value`: Person): Unit {
    encoder.encodeStructure(descriptor) {
      encodeLongElement(descriptor, 0, value._id)
      encodeStringElement(descriptor, 1, value.name)
      when (val serializer = serializersModule.getContextual(value.last_name::class)) {
        null -> encodeSerializableElement(descriptor, 2, serializer, value.last_name)
        else -> encodeStringElement(descriptor, 2, value.last_name.toString())
      }
      encodeBooleanElement(descriptor, 3, value.is_cool)
      when (val serializer = serializersModule.getContextual(value.friends::class)) {
        null -> encodeSerializableElement(descriptor, 4, serializer, value.friends)
        else -> encodeStringElement(descriptor, 4, value.friends.toString())
      }
      when (val serializer = serializersModule.getContextual(value.shhh_its_secret::class)) {
        null -> encodeSerializableElement(descriptor, 5, serializer, value.shhh_its_secret)
        else -> encodeStringElement(descriptor, 5, value.shhh_its_secret.toString())
      }
    }
  }

  public override fun deserialize(decoder: Decoder): Person = throw UnsupportedOperationException()
}
