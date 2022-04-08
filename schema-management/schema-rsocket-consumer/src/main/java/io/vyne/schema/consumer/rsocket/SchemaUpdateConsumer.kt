package io.vyne.schema.consumer.rsocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.rsocket.Payload
import io.rsocket.RSocket
import io.rsocket.core.RSocketConnector
import io.rsocket.transport.netty.client.WebsocketClientTransport
import io.rsocket.util.DefaultPayload
import io.vyne.schema.api.AddressSupplier
import io.vyne.schema.api.SchemaSet
import io.vyne.schema.rsocket.CBORJackson
import io.vyne.schema.rsocket.RSocketRoutes
import io.vyne.schema.rsocket.SchemaUpdatesRSocketFactory
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration


class RSocketSchemaUpdateConsumer(private val rsocket: SchemaUpdatesRSocketFactory, private val objectMapper:ObjectMapper = CBORJackson.defaultMapper) {
   fun subscribe(): Flux<SchemaSet> {
      return rsocket.build()
         .flatMapMany { rsocket ->
            rsocket.requestStream(DefaultPayload.create(RSocketRoutes.SCHEMA_UPDATES))
               .map { payload ->
                  objectMapper.readValue<SchemaSet>(payload.data().array())
               }
         }
   }
}
