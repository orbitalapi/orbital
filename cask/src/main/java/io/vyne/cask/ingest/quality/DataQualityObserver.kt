package io.vyne.cask.ingest.quality

import io.vyne.cask.ingest.CaskMutationDispatcher
import io.vyne.dataQuality.DataQualityRuleProvider
import io.vyne.dataQuality.DataQualitySubject
import io.vyne.dataQuality.DataSubjectQualityReportEvent
import io.vyne.dataQuality.api.DataQualityEventServiceApi
import io.vyne.schemaStore.SchemaProvider
import mu.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.scheduler.Schedulers
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
               .flatMap { evaluationResult ->
                  logger.info { "Data score: ${evaluationResult.score} - ${evaluationResult.grade}" }
                  try {
                     dataHubApi.submitQualityReportEvent(
                        DataSubjectQualityReportEvent(
                           DataQualitySubject.Message,
                           typedInstance.type.qualifiedName,
                           evaluationResult,
                           identifier = mutationMessage.attributeSet.messageId,
                           timestamp = Instant.now()
                        )
                     )
                  } catch (e: Exception) {
                     logger.warn(e) { "Failed to submit quality report event for message ${mutationMessage.attributeSet.messageId}" }
                     throw e
                  }
               }
         }
         .subscribeOn(Schedulers.boundedElastic())
         .subscribe()

   }
}
