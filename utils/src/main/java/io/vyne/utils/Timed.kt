package io.vyne.utils

import com.google.common.base.Stopwatch
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

object Timer {
   val log = KotlinLogging.logger {}
}

fun <T> xtimed(name: String, log: Boolean = false, timeUnit: TimeUnit = TimeUnit.MILLISECONDS, block: () -> T): T {
   return block()
}

fun <T> timed(name: String, log: Boolean = true, timeUnit: TimeUnit = TimeUnit.MILLISECONDS, block: () -> T): T {
   return if (log) {
      val stopwatch = Stopwatch.createStarted()
      val response = block()
      Timer.log.info { "$name completed in ${stopwatch.duration(timeUnit)}" }
      response
   } else {
      block()
   }
}

fun timed(timeUnit: TimeUnit = TimeUnit.MICROSECONDS, block: () -> Unit): Long {
   val stopwatch = Stopwatch.createStarted()
   block()
   return stopwatch.elapsed(timeUnit)
}

fun Stopwatch.duration(timeUnit: TimeUnit): String {
   val suffix = when (timeUnit) {
      TimeUnit.SECONDS -> "s"
      TimeUnit.MILLISECONDS -> "ms"
      TimeUnit.MICROSECONDS -> "μs"
      TimeUnit.NANOSECONDS -> "ns"
      else -> timeUnit.name
   }
   return "${this.elapsed(timeUnit)}$suffix"
}


fun <T> batchTimed(
   name: String,
   timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
   count: Int = 50,
   resetOnCount: Boolean = false,
   meterRegistry: MeterRegistry? = null,
   tags:Array<String> = emptyArray(),
   block: () -> T
): T {
   val recorder = TimerCounters.counters.getOrPut(name, { SamplingRecorder(name, count, resetOnCount) })
   val stopwatch = Stopwatch.createStarted()
   val result = block()
   val elapsed = stopwatch.stop()
   meterRegistry?.let {
      meterRegistry.timer(name, *tags).record(stopwatch.elapsed())
   }
   recorder.record(stopwatch.elapsed(timeUnit))
   return result
}

fun <T> xbatchTimed(
   name: String,
   timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
   count: Int = 50,
   resetOnCount: Boolean = false,
   block: () -> T
): T {
   return block()
}

private object TimerCounters {
   val counters = mutableMapOf<String, SamplingRecorder>()
}

data class SamplingRecorder(val name: String, val logOnCount: Int, val resetAfterLog: Boolean = false) {
   private val samples: Queue<Long> = ConcurrentLinkedQueue<Long>()

   fun record(value: Long) {
      samples.add(value)
      if (samples.size % logOnCount == 0) {
         log().info("Mean of $name at ${samples.size} : ${samples.average()}ms  (${samples.sum()}ms total)")
         if (resetAfterLog) {
            samples.clear()
         }
      }
   }
}
