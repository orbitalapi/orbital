package io.vyne.cask.kafka

import arrow.core.getOrHandle
import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.cask.CaskService
import io.vyne.cask.api.JsonIngestionParameters
import io.vyne.cask.websocket.JsonWebsocketRequest
import io.vyne.schemas.VersionedType
import io.vyne.utils.batchTimed
import io.vyne.utils.log
import org.apache.commons.io.IOUtils
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.util.*

@Component
@ConditionalOnProperty(
   value= ["cask.kafka.enabled"],
   havingValue = "true",
   matchIfMissing = false)
class KafkaConsumer(
   val caskService: CaskService,
   @Qualifier("ingesterMapper") val mapper: ObjectMapper
) {


   @KafkaListener(topics = ["#{'\${cask.kafka.topic}'.split(',')}"])
   fun consumeFromKafka(record: ConsumerRecord<String, String>) {
      val response = batchTimed("Do nothing") {
         val typeName = "com.cacib.m2m.Dummy"
         val versionedType = resolveType(typeName)
         val request = JsonWebsocketRequest(
            JsonIngestionParameters(),
            versionedType, mapper
         )

         val inputStream = IOUtils.toInputStream(record.value())
         val id = UUID.randomUUID().toString()

         batchTimed("Ingest from Kafka") {
            caskService.ingestRequest(
               request,
               inputStream,
               id
            )
         }
      }
//


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
