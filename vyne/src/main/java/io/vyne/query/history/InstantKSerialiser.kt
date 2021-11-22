package io.vyne.query.history

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant

object InstantKSerialiser: KSerializer<Instant> {
   override val descriptor: SerialDescriptor
      get() = PrimitiveSerialDescriptor("Instant", PrimitiveKind.LONG)

   override fun deserialize(decoder: Decoder): Instant {
      return Instant.ofEpochMilli(decoder.decodeLong())
   }

   override fun serialize(encoder: Encoder, value: Instant) {
      encoder.encodeLong(value.toEpochMilli())
   }
}
