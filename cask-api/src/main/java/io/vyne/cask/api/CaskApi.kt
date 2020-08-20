package io.vyne.cask.api

import io.vyne.schemas.VersionedType
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*
import java.time.Instant

@FeignClient("\${vyne.caskService.name:cask}")
interface CaskApi {

   @PostMapping("/api/ingest/{contentType}/{typeReference}")
   fun ingestMessage(
      @PathVariable("contentType") contentType: String,
      @PathVariable("typeReference") typeReference: String,
      @RequestParam queryParams: MultiValueMap<String, String?>,
      @RequestBody input: String): CaskIngestionResponse

   @GetMapping("/api/casks", produces = ["application/json"])
   fun getCasks(): List<CaskConfig>

   @GetMapping("/api/casks/{tableName}/details", produces = ["application/json"])
   fun getCaskDetails(@PathVariable("tableName") tableName: String): CaskDetails

   @DeleteMapping("/api/casks/{tableName}")
   fun deleteCask(@PathVariable("tableName") tableName: String)

   @PutMapping("/api/casks/{tableName}")
   fun emptyCask(@PathVariable("tableName") tableName: String)

   @PutMapping("/api/casks/{typeName}/evictSchedule", produces = ["application/json"])
   fun setEvictionSchedule(@PathVariable("typeName") typeName: String, @RequestBody parameters: EvictionScheduleParameters)

   @PostMapping("/api/casks/{typeName}/evict", produces = ["application/json"])
   fun evict(@PathVariable("typeName") typeName: String, @RequestBody parameters: EvictionParameters)
}

data class EvictionScheduleParameters(val daysToRetain: Int)
data class EvictionParameters(val writtenBefore: Instant)
