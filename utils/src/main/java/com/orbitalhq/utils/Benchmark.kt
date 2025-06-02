package com.orbitalhq.utils

import com.google.common.base.Stopwatch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object Benchmark {
   fun <T> warmup(name: String, warmup: Int = 10, useVerboseLogging: Boolean, process: (Stopwatch) -> T): List<T> {
      log().info("Starting warmup for $name")
      val results = (0 until warmup).map { count ->
         val stopWatch = Stopwatch.createStarted()
         val result = process(stopWatch)
         if (useVerboseLogging) {
            log().info("$name warmup $count of $warmup completed in ${stopWatch.elapsed(TimeUnit.MILLISECONDS)}ms")
         }

         result
      }
      log().info("Warmup finished.")
      return results
   }

   fun benchmark(
      name: String,
      warmup: Int = 10,
      iterations: Int = 50,
      timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
      useVerboseLogging: Boolean = false,
      process: (Stopwatch) -> Any
   ): BigDecimal {
      warmup(name, warmup, useVerboseLogging, process)
      val suffix = when (timeUnit) {
         TimeUnit.MILLISECONDS -> "ms"
         TimeUnit.SECONDS -> "s"
         TimeUnit.MINUTES -> "m"
         TimeUnit.NANOSECONDS -> "ns"
         TimeUnit.MICROSECONDS -> "µs"
         else -> " ${timeUnit.name}"
      }
      val executions = (0 until iterations).map { count ->
         val stopWatch = Stopwatch.createStarted()
         val result = process(stopWatch)
         val elapsed = stopWatch.elapsed(timeUnit)
         if (useVerboseLogging) {
            log().info("$name run $count of $iterations completed in ${elapsed}$suffix")
         }

         elapsed to result
      }
      val durations = executions.map { it.first }
      val collectionSize =
         executions.mapNotNull { if (it.second is Collection<*>) (it.second as Collection<*>).size else null }
      val avgSize = if (collectionSize.isNotEmpty()) " returning an average of ${
         collectionSize.average().roundToInt()
      } entries" else ""
      val averageDuration = if (durations.isNotEmpty()) durations.average().toBigDecimal()
         .setScale(2, RoundingMode.HALF_EVEN) else BigDecimal.ZERO
      if (durations.isNotEmpty()) {
         log().info("$name : Completed with average process time of $averageDuration$suffix$avgSize")
      }
      return averageDuration
   }
}
