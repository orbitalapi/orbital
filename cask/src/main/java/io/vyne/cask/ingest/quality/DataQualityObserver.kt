package io.vyne.cask.ingest.quality

import io.vyne.cask.ingest.CaskMutationDispatcher
import io.vyne.dataQuality.DataQualityRuleProvider
import io.vyne.dataQuality.DataQualitySubject
import io.vyne.dataQuality.DataSubjectQualityReportEvent
import io.vyne.dataQuality.api.DataQualityEventServiceApi
import io.vyne.schemaStore.SchemaProvider
import mu.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Component
class DataQualityObserver(
   ruleProvider: DataQualityRuleProvider,
   schemaProvider: SchemaProvider,
   mutationDispatcher: CaskMutationDispatcher,
   dataHubApi: DataQualityEventServiceApi
) {
   init {
      mutationDispatcher.flux
         .flatMap { mutationMessage ->
            val typedInstance = mutationMessage.attributeSet.toTypedInstance(schemaProvider.schema())
            ruleProvider.evaluate(typedInstance)
               .map { evaluationResult ->
                  DataSubjectQualityReportEvent(
                     DataQualitySubject.Message,
                     typedInstance.type.qualifiedName,
                     evaluationResult,
                     identifier = mutationMessage.attributeSet.messageId,
                     timestamp = Instant.now()
                  )
               }
         }.bufferTimeout(50, Duration.ofSeconds(2))
         .flatMap { events ->
            try {
               logger.info { "Submitting batch of ${events.size} quality events to the data hub" }
               dataHubApi.submitQualityReportEvent(events)
            } catch (e: Exception) {
               logger.warn(e) { "Failed to submit quality report event for message batch" }
               Flux.error(e)
            }
         }.onErrorContinue { throwable, any ->
            emptyList<String>()
         }
         .subscribeOn(Schedulers.boundedElastic())
         .subscribe {

         }

   }
}
