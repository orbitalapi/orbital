package io.vyne.cask.ingest

import reactor.core.publisher.Flux

interface PipelineSource {
    val stream: Flux<InstanceAttributeSet>
}
