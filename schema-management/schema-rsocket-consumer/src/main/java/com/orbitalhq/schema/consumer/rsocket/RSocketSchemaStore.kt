package com.orbitalhq.schema.consumer.rsocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.EmptyByteBuf
import io.rsocket.util.DefaultPayload
import com.orbitalhq.schema.api.SchemaSet
import com.orbitalhq.schema.consumer.SchemaSetChangedEventRepository
import com.orbitalhq.schema.rsocket.CBORJackson
import com.orbitalhq.schema.rsocket.RSocketRoutes
import com.orbitalhq.schema.rsocket.SchemaServerRSocketFactory
import com.orbitalhq.schemas.readers.SourceConverterRegistry
import com.orbitalhq.schemas.readers.StaticSourceConverterRegistry
import mu.KotlinLogging
import reactor.core.scheduler.Schedulers

private val logger = KotlinLogging.logger { }


class RSocketSchemaStore(
   rSocketFactory: SchemaServerRSocketFactory,
   objectMapper: ObjectMapper = CBORJackson.defaultMapper,

   // HACK: We don't strictly need this, because we can't
   // inject into the deserialization process when we recieve a new schemaSet over the wire.
   // However, injecting it here does two things:
   // a) documents the otherwise unclear relationship,
   // b) Ensures that the registry is created before this object, which triggers registration
   //    with the static provider.
   private val sourceConverterRegistry: SourceConverterRegistry = StaticSourceConverterRegistry.registry
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