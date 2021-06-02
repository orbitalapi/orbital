package io.vyne.utils

import com.google.common.base.Stopwatch
import java.math.RoundingMode
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object Benchmark {
   fun <T> warmup(name: String, warmup: Int = 10, process: (Stopwatch) -> T): List<T> {
      log().info("Starting warmup for $name")
      val results = (0..warmup).map { count ->
         val stopWatch = Stopwatch.createStarted()
         val result = process(stopWatch)
         log().info("$name warmup $count of $warmup completed in ${stopWatch.elapsed(TimeUnit.MILLISECONDS)}ms")
         result
      }
      log().info("Warmup finished.")
      return results
   }

   fun benchmark(name: String, warmup: Int = 10, iterations: Int = 50, process: (Stopwatch) -> Any) {
      warmup(name, warmup, process)
      val executions = (0..iterations).map { count ->
         val stopWatch = Stopwatch.createStarted()
         val result = process(stopWatch)
         val elapsed = stopWatch.elapsed(TimeUnit.MILLISECONDS)
         log().warn("$name run $count of $iterations completed in ${elapsed}ms")
         elapsed to result
      }
      val durations = executions.map { it.first }
      val collectionSize = executions.mapNotNull { if (it.second is Collection<*>) (it.second as Collection<*>).size else null }
      val avgSize = if (collectionSize.isNotEmpty()) " returning an average of ${collectionSize.average().roundToInt()} entries" else ""
      log().warn("Completed with average process time of ${durations.average().toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)}ms$avgSize")
   }
}
