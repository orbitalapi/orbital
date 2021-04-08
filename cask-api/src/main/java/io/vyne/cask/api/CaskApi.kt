package io.vyne.cask.api

import org.springframework.web.bind.annotation.*
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Mono

data class CsvIngestionParameters(
   val delimiter: Char = ',',
   val firstRecordAsHeader: Boolean = true,
   val nullValue: Set<String> = emptySet(),
   val ignoreContentBefore: String? = null,
   val containsTrailingDelimiters: Boolean = false,
   val debug: Boolean = false
)
data class JsonIngestionParameters(
   val debug: Boolean = false
)

data class XmlIngestionParameters(
   val debug: Boolean = false,
   val elementSelector: String? = null
)

enum class ContentType { json, csv, xml }

@ReactiveFeignClient("\${vyne.caskService.name:cask}")
interface CaskApi {

   @PostMapping("/api/ingest/csv/{typeReference}")
   fun ingestCsv(@PathVariable("typeReference") typeReference: String,
      // Note: While spring supports parsing parameters to POJO's, feign doesn't,
      // so we're left with this litany of input parameters, which is weak as. Grr.
//                 parameters: CsvIngestionParameters = CsvIngestionParameters(),
                 @RequestParam("delimiter", defaultValue = ",") delimiter: Char = ',',
                 @RequestParam("firstRecordAsHeader", defaultValue = "true") firstRecordAsHeader: Boolean = true,
                 @RequestParam("nullValue", required = false, defaultValue = "") nullValue: Set<String> = emptySet(),
                 @RequestParam("ignoreContentBefore", required = false) ignoreContentBefore: String? = null,
                 @RequestParam("containsTrailingDelimiters", required = false, defaultValue = "false") containsTrailingDelimiters: Boolean = false,
                 @RequestParam("debug", defaultValue = "false") debug: Boolean = false,
                 @RequestBody input: String): Mono<CaskIngestionResponse>

   @PostMapping("/api/ingest/json/{typeReference}")
   fun ingestJson(@PathVariable("typeReference") typeReference: String,
      // Note: While spring supports parsing parameters to POJO's, feign doesn't,
      // so we're left with this litany of input parameters, which is weak as. Grr.
//                  parameters: JsonIngestionParameters = JsonIngestionParameters(),
                  @RequestParam("debug", defaultValue = "false") debug: Boolean = false,
                  @RequestBody input: String): Mono<CaskIngestionResponse>

   @PostMapping("/api/ingest/xml/{typeReference}")
   fun ingestXml(@PathVariable("typeReference") typeReference: String,
      // Note: While spring supports parsing parameters to POJO's, feign doesn't,
      // so we're left with this litany of input parameters, which is weak as. Grr.
//                  parameters: JsonIngestionParameters = JsonIngestionParameters(),
                  @RequestParam("debug", defaultValue = "false") debug: Boolean = false,
                 @RequestParam("elementSelector", required = false) elementSelector: String? = null,
                  @RequestBody input: String): Mono<CaskIngestionResponse>

   @GetMapping("/api/casks", produces = ["application/json"])
   fun getCasks(): Mono<List<CaskConfig>>

   @GetMapping("/api/casks/{tableName}/details", produces = ["application/json"])
   fun getCaskDetails(@PathVariable("tableName") tableName: String): Mono<CaskDetails>

   @PostMapping("/api/casks/{tableName}/errors", produces = ["application/json"])
   fun getCaskIngestionErrors(@PathVariable("tableName") tableName: String,
                              @RequestBody request: CaskIngestionErrorsRequestDto): Mono<CaskIngestionErrorDtoPage>

   @GetMapping("/api/casks/{caskMessageId}")
   fun getIngestionMessage(@PathVariable caskMessageId: String): Mono<String>

   @DeleteMapping("/api/casks/{tableName}")
   fun deleteCask(@PathVariable("tableName") tableName: String, @RequestParam(defaultValue = "false", required = false) force: Boolean = false): Mono<String>

   @DeleteMapping("/api/types/cask/{typeName}")
   fun deleteCaskByTypeName(@PathVariable("typeName") typeName: String, @RequestParam(defaultValue = "false", required = false) force: Boolean = false): Mono<String>
}
