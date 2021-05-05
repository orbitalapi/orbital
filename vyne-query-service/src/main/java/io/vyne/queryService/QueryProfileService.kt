package io.vyne.queryService

import io.vyne.utils.StrategyPerformanceProfiler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class QueryProfileService {

   @GetMapping("/api/profile")
   fun getLastProfile(): StrategyPerformanceProfiler.SearchStrategySummary {
      return StrategyPerformanceProfiler.summarizeAndReset()
   }
}
