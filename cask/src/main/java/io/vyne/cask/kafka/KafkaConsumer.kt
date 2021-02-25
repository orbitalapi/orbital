package io.vyne.cask.kafka

import arrow.core.getOrHandle
import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.cask.CaskService
import io.vyne.cask.api.JsonIngestionParameters
import io.vyne.cask.api.XmlIngestionParameters
import io.vyne.cask.ingest.IngestionInitialisedEvent
import io.vyne.cask.websocket.JsonWebsocketRequest
import io.vyne.cask.websocket.XmlWebsocketRequest
import io.vyne.query.graph.type
import io.vyne.schemas.VersionedType
import io.vyne.utils.batchTimed
import io.vyne.utils.log
import org.apache.commons.io.IOUtils
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationEventPublisher
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.lang.IllegalArgumentException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

@Component
@ConditionalOnProperty(
   value= ["cask.kafka.enabled"],
   havingValue = "true",
   matchIfMissing = false)
class KafkaConsumer(
   val caskService: CaskService,
   @Qualifier("ingesterMapper") val mapper: ObjectMapper,
   @Value("\${cask.kafka.type-name}") private val typeName: String,
   @Value("\${cask.kafka.ingestion-type}") private val ingestionType: String,
   private val applicationEventPublisher: ApplicationEventPublisher
) {

  private val publishedIngestionInitialisedEvent = AtomicBoolean(false)

   @KafkaListener(topics = ["#{'\${cask.kafka.topic}'.split(',')}"], concurrency = "\${cask.kafka.consumer-count}")
   fun consumeFromKafka(record: ConsumerRecord<String, String>) {
      if (!publishedIngestionInitialisedEvent.get()) {
         applicationEventPublisher.publishEvent(IngestionInitialisedEvent(this, resolveType(typeName)))
         publishedIngestionInitialisedEvent.set(true)
      }

      val response = batchTimed("Do nothing") {
         val versionedType = resolveType(typeName)
         val request = when(ingestionType) {
            "xml" -> XmlWebsocketRequest(XmlIngestionParameters(), versionedType)
            "json" -> JsonWebsocketRequest(JsonIngestionParameters(), versionedType, mapper)
            else -> throw IllegalArgumentException("Unsupported Ingestion Type")
         }

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
