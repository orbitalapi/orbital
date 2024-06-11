package com.orbitalhq.cockpit.core.monitoring

import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

enum class MetricsWindow(private val rangeProvider: (ZonedDateTime) -> ClosedRange<Instant>) {
   Last30Seconds(forDuration( Duration.ofSeconds(30))),
   LastMinute(forDuration( Duration.ofMinutes(1))),
   Last5Minutes(forDuration( Duration.ofMinutes(5))),
   LastHour(forDuration( Duration.ofMinutes(60))),
   Last4Hours(forDuration( Duration.ofHours(4))),
   LastDay(forDuration( Duration.ofDays(1))),
   Last7Days(forDuration( Duration.ofDays(7))),
   Last30Days(forDuration( Duration.ofDays(30))),
   MonthToDate({ now ->
      now.atStartOfTomorrow().rangeBackTo { end -> end.atStartOfMonth(now.offset) }
   }),
   YearToDate({ now ->
      now.atStartOfTomorrow().rangeBackTo { end -> end.atStartOfYear(now.offset) }
   });

   fun asRange(now: ZonedDateTime = ZonedDateTime.now()):ClosedRange<Instant> {
      return this.rangeProvider(now)
   }
}

private fun forDuration(duration: Duration): (ZonedDateTime) -> ClosedRange<Instant> {
   return { now: ZonedDateTime ->
      now.toInstant().rangeBackTo { it.minus(duration) }
   }
}

private fun Instant.atStartOfYear(offset: ZoneOffset): Instant {
   return this.atZone(offset)
      .toLocalDate()
      .withDayOfYear(1)
      .atStartOfDay()
      .toInstant(offset)
}

private fun Instant.atStartOfMonth(offset: ZoneOffset): Instant {
   return this.atZone(offset)
      .toLocalDate()
      .withDayOfMonth(1)
      .atStartOfDay()
      .toInstant(offset)
}

private fun ZonedDateTime.atStartOfTomorrow(): Instant {
   return this.toLocalDateTime().toLocalDate().atStartOfDay().plusDays(1L)
      .toInstant(this.offset)
}

private fun Instant.rangeBackTo(calculator: (Instant) -> Instant): ClosedRange<Instant> {
   val start = calculator(this)
   return start.rangeTo(this)
}
