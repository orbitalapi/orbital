package com.orbitalhq.queryService

import com.orbitalhq.utils.TimeBucketed
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class DebugService {

   @GetMapping("/api/debugLog")
   fun logTimeBuckets() {
      TimeBucketed.DEFAULT.log()
   }
}
