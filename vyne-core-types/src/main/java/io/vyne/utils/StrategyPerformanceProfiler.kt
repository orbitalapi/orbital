package io.vyne.utils

import com.google.common.base.Stopwatch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

object StrategyPerformanceProfiler {
   fun <T> profiled(key:String, operation: () -> T):T {
      val sw = Stopwatch.createStarted()
      val result = operation()
      record(key,sw.elapsed())
      return result
   }
   data class AtomicSum(private val sum :AtomicLong = AtomicLong(0), private val count:AtomicInteger = AtomicInteger(0)) {
      fun incremet(value:Long) {
         sum.addAndGet(value)
         count.incrementAndGet()
      }
      fun count() = count.get()
      fun sum() = sum.get()
   }
   private val values = mutableMapOf<String,AtomicSum>()
   fun record(operation:String, duration: Duration) {
         values.getOrPut(operation, { AtomicSum() }).incremet(duration.toNanos())
   }
   fun summarizeAndReset(): SearchStrategySummary {
      val withResultsSummary = generateSummary()
//      val withoutResultsSummary = generateSummary(withoutResults)
      val summary = SearchStrategySummary(withResultsSummary)
      values.clear()
      return summary
   }

   private fun generateSummary(): Map<String, StatisiticsSummary> {
      val summaries = values.mapValues { (_,atomicSum) ->
         val total = atomicSum.sum() / 1_000_000
         val count = atomicSum.count()
         val mean = total.toBigDecimal().divide(count.toBigDecimal(), 4, RoundingMode.HALF_UP)
         StatisiticsSummary(count, total.toInt(), mean)
      }.toMap()
      return summaries
   }

   data class StatisiticsSummary(
      val count: Int, val totalMillis: Int, val mean: BigDecimal
   )

   data class SearchStrategySummary(
      val results: Map<String, StatisiticsSummary>
   ) {
      override fun toString(): String {
         return results.map { (k,v) -> "$k : $v" }
            .joinToString("\n")
      }
   }
}
