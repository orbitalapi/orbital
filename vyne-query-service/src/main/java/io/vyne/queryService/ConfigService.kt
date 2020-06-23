package io.vyne.queryService

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ConfigService(private val config: QueryServerConfig) {

   @GetMapping("/api/config")
   fun getConfig(): QueryServerConfig {
      return config
   }
}
