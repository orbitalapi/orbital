package io.vyne.cask.ingest.quality

import io.vyne.cask.ingest.InstanceAttributeSet
import io.vyne.dataQuality.DataQualityRuleProvider
import io.vyne.schemaStore.SchemaProvider
import mu.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Sinks

private val logger = KotlinLogging.logger {}

@Component
class DataQualityObserver(
   ruleProvider: DataQualityRuleProvider,
   schemaProvider: SchemaProvider
) : DataQualitySinkSource {
   override val observerSink: Sinks.Many<InstanceAttributeSet> =
      Sinks.many().multicast().onBackpressureBuffer<InstanceAttributeSet>()

   init {
       observerSink.asFlux()
          .subscribe { instanceAttributeSet ->
             val typedInstance = instanceAttributeSet.toTypedInstance(schemaProvider.schema())
             val evaluationResult = ruleProvider.evaluate(typedInstance)
             logger.info { "Data score: ${evaluationResult.score} - ${evaluationResult.grade}" }
          }
   }
}


interface DataQualitySinkSource {
   val observerSink: Sinks.Many<InstanceAttributeSet>
}
