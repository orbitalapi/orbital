package io.vyne.cask.ingest

import reactor.core.publisher.Flux

interface StreamSource {
   val records:List<InstanceAttributeSet>
      get() { TODO() }
   val stream: Flux<InstanceAttributeSet>
   val messageId: String
}
