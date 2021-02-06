package io.vyne.cask.kafka

import arrow.core.getOrHandle
import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.cask.CaskService
import io.vyne.cask.api.JsonIngestionParameters
import io.vyne.cask.batchTimed
import io.vyne.cask.websocket.JsonWebsocketRequest
import io.vyne.schemas.VersionedType
import io.vyne.utils.log
import org.apache.commons.io.IOUtils
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

@Component
class KafkaConsumer(
   val caskService: CaskService,
   @Qualifier("ingesterMapper") val mapper: ObjectMapper
) {

   @KafkaListener(topics = ["load-test"])
   fun consumeFromKafka(record: ConsumerRecord<String, String>) {
      val typeName = "com.cacib.m2m.Dummy"
      val versionedType = resolveType(typeName)
      batchTimed("Ingest from Kafka") {
         caskService.ingestRequest(
            JsonWebsocketRequest(
               JsonIngestionParameters(),
               versionedType, mapper
            ),
            Flux.just(IOUtils.toInputStream(record.value()))
         ).subscribe()
      }

   }

   private fun resolveType(typeName: String): VersionedType {
      return caskService.resolveType(typeName)
         .getOrHandle {
            log().error("Failed to resolve type $typeName.  Blocking for a bit, then will try again")
            Thread.sleep(2500)
            resolveType(typeName)
         }
   }
}
