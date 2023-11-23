package com.orbitalhq.metrics

import java.time.Duration

interface QueryMetricsReporter {
   fun firstResult(duration: Duration)
   fun completed(duration: Duration, finalCount: Int)
   fun failed(duration:Duration)
}

object NoOpMetricsReporter : QueryMetricsReporter {
   override fun firstResult(duration: Duration) {
   }

   override fun completed(duration: Duration, finalCount: Int) {
   }

   override fun failed(duration: Duration) {
   }

}
