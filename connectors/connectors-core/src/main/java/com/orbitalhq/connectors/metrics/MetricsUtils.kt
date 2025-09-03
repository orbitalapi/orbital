package com.orbitalhq.connectors.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import reactor.core.observability.micrometer.Micrometer
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

fun <T> Mono<T>.tags(tags: List<Tag>): Mono<T> {
   if (tags.isEmpty()) return this
   return tags.fold(this) { mono, tag -> mono.tag(tag.key, tag.value)}
}

fun <T> Flux<T>.tags(tags: List<Tag>): Flux<T> {
   if (tags.isEmpty()) return this
   return tags.fold(this) { mono, tag -> mono.tag(tag.key, tag.value)}
}

/**
 * Convenience to capture metrics from a Mono<>
 */
fun <T> Mono<T>.captureMetrics(tags: List<Tag>, meterRegistry: MeterRegistry): Mono<T> {
   return this.tags(tags)
      .tap(Micrometer.metrics(meterRegistry))
}

fun <T> Flux<T>.captureMetrics(tags: List<Tag>, meterRegistry: MeterRegistry): Flux<T> {
   return this.tags(tags)
      .tap(Micrometer.metrics(meterRegistry))
}
