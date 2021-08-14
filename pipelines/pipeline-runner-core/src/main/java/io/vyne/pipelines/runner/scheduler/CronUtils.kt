package io.vyne.pipelines.runner.scheduler

import io.vyne.pipelines.runner.transport.http.CronExpression
import org.springframework.scheduling.support.CronSequenceGenerator
import reactor.core.publisher.Flux
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Date

/**
 * Contains a flux that emits a signal as per the provided cron schedule.
 *
 * @param tickFrequency defines how often the underlying schedule is checked
 *
 */
class CronSchedule(
   val schedule: CronExpression,
   private val tickFrequency: Duration = Duration.ofSeconds(1),
   private val clock: Clock = Clock.systemUTC()
) {
   private fun now(): Instant = Instant.now(clock)
   var lastMatched = now()
      private set

   private val generator: CronSequenceGenerator = CronSequenceGenerator(schedule)

   val flux: Flux<Instant> = Flux.interval(tickFrequency)
      .map { now() }
      .filter { tick ->
         val nextScheduledEvent = generator.next(Date.from(lastMatched)).toInstant()
         nextScheduledEvent.isBefore(tick) || nextScheduledEvent == (tick)
      }
      .doOnEach { lastMatched = it.get() ?: now() }
}
