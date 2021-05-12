package io.vyne.cask.ingest

import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

interface StreamSource {
   val stream: Flux<InstanceAttributeSet>
   val messageId: String

   fun withObserver(sink: Sinks.Many<InstanceAttributeSet>):StreamSource
}
