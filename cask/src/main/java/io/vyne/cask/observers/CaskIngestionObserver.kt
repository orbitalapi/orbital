package io.vyne.cask.observers

import com.google.common.cache.CacheBuilder
import io.vyne.cask.ingest.CaskMutationDispatcher
import io.vyne.cask.ingest.TaxiAnnotationHelper
import io.vyne.cask.observers.kafka.KafkaTemplateFactory
import io.vyne.cask.observers.kafka.KafkaMessageWriter
import io.vyne.schema.api.SchemaProvider
import lang.taxi.types.ObjectType
import lang.taxi.types.Type
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.UUID

private val logger = KotlinLogging.logger {}
@Component
/**
 * Observes CaskEntityMutating messages and publishing the ones for models annotated with @ObserveChanges
 * to Kafka.
 */
class CaskIngestionObserver(
   observerConfigurationProperties: IngestionObserverConfigurationProperties = IngestionObserverConfigurationProperties(listOf()),
   caskMutationDispatcher: CaskMutationDispatcher,
   private val schemaProvider: SchemaProvider
) : IngestionObserver {

   private val nameToConfigMap = observerConfigurationProperties.kafka.map { it.connectionName to it }.toMap()
   private val cache = CacheBuilder.newBuilder().weakValues().build<String, KafkaMessageWriter>()

   init {
      caskMutationDispatcher.flux.subscribe { mutatingMessage ->
         val writeToConnectionName = mutatingMessage.writeToConnectionName
         if (writeToConnectionName != null && nameToConfigMap[writeToConnectionName] != null) {
            val kafkaConfig = nameToConfigMap[writeToConnectionName]!!
            val sender = cache.get(kafkaConfig.bootstrapServers) {
               KafkaMessageWriter(KafkaTemplateFactory.kafkaTemplateForBootstrapServers(kafkaConfig.bootstrapServers))
            }
            try {
               sender.send(UUID.randomUUID().toString(), ObservedChange.fromCaskEntityMutatingMessage(schemaProvider.schema, mutatingMessage), kafkaConfig.topic)
            } catch (e: Exception) {
               logger.error(e) { "Error publishing an observed change for $mutatingMessage" }
            }
         }
      }
   }

   override fun isObservable(taxiType: Type): Boolean {
      return observerConfigurationForTaxiType(taxiType) != null
   }

   override fun kafkaObserverConfig(taxiType: Type): KafkaObserverConfiguration {
      return observerConfigurationForTaxiType(taxiType)!!
   }

   private fun observerConfigurationForTaxiType(taxiType: Type) =
      TaxiAnnotationHelper.observeChangesConnectionName(taxiType as ObjectType)?.let { nameToConfigMap[it] }
}
