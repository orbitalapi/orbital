package io.vyne.cask.api

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*

@FeignClient("cask")
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
}
