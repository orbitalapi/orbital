package io.vyne.history.codec

import io.vyne.query.history.VyneHistoryRecord
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import org.reactivestreams.Publisher
import org.springframework.core.ResolvableType
import org.springframework.core.codec.AbstractEncoder
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.http.MediaType
import org.springframework.util.MimeType
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class VyneHistoryRecordObjectEncoder: AbstractEncoder<VyneHistoryRecord>(MediaType.APPLICATION_CBOR) {

   override fun encodeValue(value: VyneHistoryRecord,
                            dataBufferFactory: DataBufferFactory,
                            valueType: ResolvableType,
                            mimeType: MimeType?,
                            hints: MutableMap<String, Any>?): DataBuffer {

      // 'Serializers are being looked for in a [SerializersModule] from the target [Encoder] or [Decoder], using statically known [KClass]"
      val byteArray = Cbor.encodeToByteArray(value)
      val buffer: DataBuffer = dataBufferFactory.allocateBuffer(byteArray.size)
      buffer.write(byteArray)
      return buffer
   }
   override fun encode(inputStream: Publisher<out VyneHistoryRecord>,
                       dataBufferFactory: DataBufferFactory,
                       resolvableType: ResolvableType,
                       mimeType: MimeType,
                       hints: MutableMap<String, Any>?): Flux<DataBuffer> {
      return if (inputStream is Mono) {
         Mono.from(inputStream).map { value ->
            val byteArray = Cbor.encodeToByteArray(value)
            val buffer: DataBuffer = dataBufferFactory.allocateBuffer(byteArray.size)
            buffer.write(byteArray)
         }.flux()
      } else {
         Flux.from(inputStream).collectList().map { list ->
            val byteArray = Cbor.encodeToByteArray(list)
            val buffer: DataBuffer = dataBufferFactory.allocateBuffer(byteArray.size)
            buffer.write(byteArray)
         }.flux()
      }
   }
}
