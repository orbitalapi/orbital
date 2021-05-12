package io.vyne.cask.ingest.quality

import io.vyne.cask.ingest.CaskMutationDispatcher
import io.vyne.dataQuality.DataQualityRuleProvider
import io.vyne.schemaStore.SchemaProvider
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class DataQualityObserver(
   ruleProvider: DataQualityRuleProvider,
   schemaProvider: SchemaProvider,
   mutationDispatcher: CaskMutationDispatcher
) {

   init {
      mutationDispatcher.flux()
         .subscribe { mutationMessage ->
            val typedInstance = mutationMessage.attributeSet.toTypedInstance(schemaProvider.schema())
            val evaluationResult = ruleProvider.evaluate(typedInstance)
            logger.info { "Data score: ${evaluationResult.score} - ${evaluationResult.grade}" }
         }
   }
}
