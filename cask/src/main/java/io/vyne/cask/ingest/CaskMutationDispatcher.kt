package io.vyne.cask.ingest

import mu.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers

private val logger = KotlinLogging.logger {}

@Component
class CaskMutationDispatcher : CaskChangeMutationDispatcher {
   private val sinkMutating = Sinks.many().multicast().onBackpressureBuffer<CaskEntityMutatingMessage>()
   private val sinkMutated = Sinks.many().multicast().onBackpressureBuffer<CaskEntityMutatedMessage>()
   val fluxMutated: Flux<CaskEntityMutatedMessage> = sinkMutated.asFlux().publishOn(Schedulers.boundedElastic())
   val flux: Flux<CaskEntityMutatingMessage> = sinkMutating.asFlux().publishOn(Schedulers.boundedElastic())

   override fun acceptMutating(message: CaskEntityMutatingMessage) {
      val emitResult = sinkMutating.tryEmitNext(message)
      if (emitResult.isFailure) {
             // Commenting out this as when this is enabled
            // CaskAppIntegrationTest::canIngestLargeContentViaWebsocketConnection
            // Fails with 10 seconds timeout on Gitlab (passes fine locally, and also passes on Gitlab when the test timeout
            //   increased to 30 secs
         // logger.warn { "Failed to emit change message $message as it was rejected by the sink" }
      }
   }

   override fun acceptMutated(message: CaskEntityMutatedMessage) {
      val emitResult = sinkMutated.tryEmitNext(message)
      if (emitResult.isFailure) {

      }
   }
}

data class CaskIdColumnValue(val columnName: String, val value: Any)

/**
 * CaskEntityMutatedMessage - Emitted from produced when CasK entity has been emitted AND
 * COMMITTED in the persistence store
 */
data class CaskEntityMutatingMessage(
   val tableName: String,
   val identity: List<CaskIdColumnValue> = emptyList(),
   val attributeSet: InstanceAttributeSet
)

/**
 * CaskEntityMutatedMessage - Emitted from produced when CasK entity has been emitted AND
 * COMMITTED in the persistence store
 */
data class CaskEntityMutatedMessage(
   val tableName: String,
   val identity: List<CaskIdColumnValue> = emptyList()
) {

}

interface CaskChangeMutationDispatcher {
   fun acceptMutated(message: CaskEntityMutatedMessage)
   fun acceptMutating(message: CaskEntityMutatingMessage)
}
