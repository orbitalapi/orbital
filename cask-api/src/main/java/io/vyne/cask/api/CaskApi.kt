package io.vyne.cask.api

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

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

@FeignClient("\${vyne.caskService.name:cask}")
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
                 @RequestBody input: String): CaskIngestionResponse

   @PostMapping("/api/ingest/json/{typeReference}")
   fun ingestJson(@PathVariable("typeReference") typeReference: String,
      // Note: While spring supports parsing parameters to POJO's, feign doesn't,
      // so we're left with this litany of input parameters, which is weak as. Grr.
//                  parameters: JsonIngestionParameters = JsonIngestionParameters(),
                  @RequestParam("debug", defaultValue = "false") debug: Boolean = false,
                  @RequestBody input: String): CaskIngestionResponse

   @PostMapping("/api/ingest/xml/{typeReference}")
   fun ingestXml(@PathVariable("typeReference") typeReference: String,
      // Note: While spring supports parsing parameters to POJO's, feign doesn't,
      // so we're left with this litany of input parameters, which is weak as. Grr.
//                  parameters: JsonIngestionParameters = JsonIngestionParameters(),
                  @RequestParam("debug", defaultValue = "false") debug: Boolean = false,
                 @RequestParam("elementSelector", required = false) elementSelector: String? = null,
                  @RequestBody input: String): CaskIngestionResponse

   @GetMapping("/api/casks", produces = ["application/json"])
   fun getCasks(): List<CaskConfig>

   @GetMapping("/api/casks/{tableName}/details", produces = ["application/json"])
   fun getCaskDetails(@PathVariable("tableName") tableName: String): CaskDetails

   @DeleteMapping("/api/casks/{tableName}")
   fun deleteCask(@PathVariable("tableName") tableName: String)

   @PutMapping("/api/casks/{tableName}")
   fun emptyCask(@PathVariable("tableName") tableName: String)

   @PostMapping("/api/casks/{tableName}/errors", produces = ["application/json"])
   fun getCaskIngestionErrors(@PathVariable("tableName") tableName: String,
                              @RequestBody request: CaskIngestionErrorsRequestDto): CaskIngestionErrorDtoPage

   @GetMapping("/api/casks/{caskMessageId}")
   fun getIngestionMessage(@PathVariable caskMessageId: String): ResponseEntity<Resource>
}
