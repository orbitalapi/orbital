package io.vyne.history.codec

import io.vyne.query.history.VyneHistoryRecord
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import org.reactivestreams.Publisher
import org.springframework.core.ResolvableType
import org.springframework.core.codec.AbstractDecoder
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.MediaType
import org.springframework.util.MimeType
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.SynchronousSink

class VyneHistoryRecordDecoder: AbstractDecoder<VyneHistoryRecord>(MediaType.APPLICATION_CBOR) {
   override fun decode(
      input: Publisher<DataBuffer>,
      elementType: ResolvableType,
      mimeType: MimeType?,
      hints: MutableMap<String, Any>?): Flux<VyneHistoryRecord> {
      return Flux.from(input).handle { buffer, sink: SynchronousSink<VyneHistoryRecord> ->
         val value = Cbor.decodeFromByteArray<VyneHistoryRecord>(buffer.asByteBuffer().array())
         sink.next(value)
      }
   }

   override fun decodeToMono(inputStream: Publisher<DataBuffer>, elementType: ResolvableType, mimeType: MimeType?, hints: MutableMap<String, Any>?): Mono<VyneHistoryRecord> {
      return Mono.from(inputStream).handle { buffer, sink: SynchronousSink<VyneHistoryRecord> ->
         val value = Cbor.decodeFromByteArray<VyneHistoryRecord>(buffer.asByteBuffer().array())
         sink.next(value)
      }
   }
}
