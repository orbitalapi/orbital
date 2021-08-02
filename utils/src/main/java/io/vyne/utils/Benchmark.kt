package io.vyne.utils

import com.google.common.base.Stopwatch
import java.math.RoundingMode
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object Benchmark {
   fun <T> warmup(name: String, warmup: Int = 10,logIterations:Boolean = false,  process: (Stopwatch) -> T): List<T> {
      log().info("Starting warmup for $name")
      val results = (0 until warmup).map { count ->
         val stopWatch = Stopwatch.createStarted()
         val result = process(stopWatch)
         if (logIterations) {
            log().info("$name warmup $count of $warmup completed in ${stopWatch.elapsed(TimeUnit.MILLISECONDS)}ms")
         }
         result
      }
      log().info("Warmup finished.")
      return results
   }

   fun benchmark(name: String, warmup: Int = 10, iterations: Int = 50, logIterations:Boolean = false, process: (Stopwatch) -> Any) {
      warmup(name, warmup, logIterations, process)
      val executions = (0 until iterations).map { count ->
         val stopWatch = Stopwatch.createStarted()
         val result = process(stopWatch)
         val elapsed = stopWatch.elapsed(TimeUnit.MILLISECONDS)
         if (logIterations) {
            log().warn("$name run $count of $iterations completed in ${elapsed}ms")
         }

         elapsed to result
      }
      val durations = executions.map { it.first }
      val collectionSize = executions.mapNotNull { if (it.second is Collection<*>) (it.second as Collection<*>).size else null }
      val avgSize = if (collectionSize.isNotEmpty()) " returning an average of ${collectionSize.average().roundToInt()} entries" else ""
      if (durations.isNotEmpty()) {
         log().warn("Completed with average process time of ${durations.average().toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)}ms$avgSize")
      }

   }
}
