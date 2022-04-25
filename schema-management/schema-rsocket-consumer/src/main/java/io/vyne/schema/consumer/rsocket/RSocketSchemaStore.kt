package io.vyne.schema.consumer.rsocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.rsocket.util.DefaultPayload
import io.vyne.schema.api.SchemaSet
import io.vyne.schema.consumer.SchemaSetChangedEventRepository
import io.vyne.schema.rsocket.CBORJackson
import io.vyne.schema.rsocket.RSocketRoutes
import io.vyne.schema.rsocket.SchemaServerRSocketFactory
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }


class RSocketSchemaStore(
   rSocketFactory: SchemaServerRSocketFactory,
   objectMapper: ObjectMapper = CBORJackson.defaultMapper
) : SchemaSetChangedEventRepository() {

   private val logger = KotlinLogging.logger {}
   init {
      rSocketFactory.rsockets
         .flatMap { rsocket ->
            logger.info { "Received new RSocket connection, subscribing for schema updates" }
            rsocket.requestStream(DefaultPayload.create(RSocketRoutes.SCHEMA_UPDATES))
               .map { payload ->
                  logger.info { "Received updated schema over RSocket connection" }
                  objectMapper.readValue<SchemaSet>(payload.data().array())
               }
         }.subscribe { newSchemaSet ->
            emitNewSchemaIfDifferent(newSchemaSet)
         }


   }

}
