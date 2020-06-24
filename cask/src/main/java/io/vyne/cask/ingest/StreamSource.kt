package io.vyne.cask.ingest

import reactor.core.publisher.Flux

interface StreamSource {
    val stream: Flux<InstanceAttributeSet>
}
