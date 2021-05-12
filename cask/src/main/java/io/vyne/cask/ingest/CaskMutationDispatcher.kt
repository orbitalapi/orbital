package io.vyne.cask.ingest

import mu.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Sinks

private val logger = KotlinLogging.logger {}

@Component
class CaskMutationDispatcher : CaskChangeMutationDispatcher {
   private val sink = Sinks.many().multicast().onBackpressureBuffer<CaskEntityMutatedMessage>()
   override fun accept(message: CaskEntityMutatedMessage) {
      val emitResult = sink.tryEmitNext(message)
      if (emitResult.isFailure) {
         logger.warn { "Failed to emit change message $message as it was rejected by the sink" }
      }
   }
}

data class CaskEntityMutatedMessage(
   val caskName: String,
   val tableName: String? = null, // serhat, make these non-nullable once your work is done
   val identity: List<CaskIdColumnValue> = emptyList(),
   val attributeSet: InstanceAttributeSet
) {
   data class CaskIdColumnValue(val columnName: String, val value: Any)
}

interface CaskChangeMutationDispatcher {
   fun accept(message: CaskEntityMutatedMessage)
}
