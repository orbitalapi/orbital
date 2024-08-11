package com.orbitalhq.queryService

import com.orbitalhq.security.VynePrivileges
import com.orbitalhq.utils.StrategyPerformanceProfiler
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class QueryProfileService {

   @PreAuthorize("hasAuthority('${VynePrivileges.RunQuery}')")
   fun getLastProfile(): StrategyPerformanceProfiler.SearchStrategySummary {
      return StrategyPerformanceProfiler.summarizeAndReset()
   }
}
