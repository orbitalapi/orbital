package io.vyne.schema.consumer.rsocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.EmptyByteBuf
import io.rsocket.util.DefaultPayload
import io.vyne.schema.api.SchemaSet
import io.vyne.schema.consumer.SchemaSetChangedEventRepository
import io.vyne.schema.rsocket.CBORJackson
import io.vyne.schema.rsocket.RSocketRoutes
import io.vyne.schema.rsocket.SchemaServerRSocketFactory
import mu.KotlinLogging
import reactor.core.scheduler.Schedulers

private val logger = KotlinLogging.logger { }


class RSocketSchemaStore(
   rSocketFactory: SchemaServerRSocketFactory,
   objectMapper: ObjectMapper = CBORJackson.defaultMapper
) : SchemaSetChangedEventRepository() {

   private val logger = KotlinLogging.logger {}

   init {
      rSocketFactory.rsockets
         .subscribe { rsocket ->
            rsocket.onClose()
               .doFinally { signal ->
                  logger.info { "Schema consumer rsocket connection was terminated.  Waiting for a new connection" }
               }
               .subscribe()
            logger.info { "Received new RSocket connection, subscribing for schema updates" }
            rsocket.requestStream(
               DefaultPayload.create(EmptyByteBuf(ByteBufAllocator.DEFAULT), RSocketRoutes.schemaUpdatesRouteMetadata())
            ).publishOn(Schedulers.boundedElastic())
               .map { payload ->
                  logger.info { "Received updated schema over RSocket connection" }
                  objectMapper.readValue<SchemaSet>(payload.data().array())
               }.subscribe { newSchemaSet ->
                  emitNewSchemaIfDifferent(newSchemaSet)
               }

         }
   }

}
