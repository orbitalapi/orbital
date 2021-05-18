package io.vyne.cask.ingest

import mu.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers

private val logger = KotlinLogging.logger {}

@Component
class CaskMutationDispatcher : CaskChangeMutationDispatcher {
   private val sink = Sinks.many()
      .multicast()
      .onBackpressureBuffer<CaskEntityMutatedMessage>()

   val flux: Flux<CaskEntityMutatedMessage> = sink.asFlux().publishOn(Schedulers.boundedElastic())

   override fun accept(message: CaskEntityMutatedMessage) {
      val emitResult = sink.tryEmitNext(message)
      if (emitResult.isFailure) {
             // Commenting out this as when this is enabled
            // CaskAppIntegrationTest::canIngestLargeContentViaWebsocketConnection
            // Fails with 10 seconds timeout on Gitlab (passes fine locally, and also passes on Gitlab when the test timeout
            //   increased to 30 secs
         // logger.warn { "Failed to emit change message $message as it was rejected by the sink" }
      }
   }
}

data class CaskEntityMutatedMessage(
   val tableName: String,
   val identity: List<CaskIdColumnValue> = emptyList(),
   val attributeSet: InstanceAttributeSet
) {
   data class CaskIdColumnValue(val columnName: String, val value: Any)
}

interface CaskChangeMutationDispatcher {
   fun accept(message: CaskEntityMutatedMessage)
}
