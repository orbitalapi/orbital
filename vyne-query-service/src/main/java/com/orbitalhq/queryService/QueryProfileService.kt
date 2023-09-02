package com.orbitalhq.queryService

import com.orbitalhq.utils.StrategyPerformanceProfiler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class QueryProfileService {

   @GetMapping("/api/profile")
   fun getLastProfile(): StrategyPerformanceProfiler.SearchStrategySummary {
      return StrategyPerformanceProfiler.summarizeAndReset()
   }
}
