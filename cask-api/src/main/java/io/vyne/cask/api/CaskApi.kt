package io.vyne.cask.api

import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Mono
import java.time.Instant


data class JsonIngestionParameters(
   val debug: Boolean = false
)

data class XmlIngestionParameters(
   val debug: Boolean = false,
   val elementSelector: String? = null
)

data class EvictionScheduleParameters(val daysToRetain: Int)
data class EvictionParameters(val writtenBefore: Instant)

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
   fun deleteCask(@PathVariable("tableName") tableName: String, @RequestParam(defaultValue = "false", required = false) force: Boolean = false): Mono<CaskConfig?>

   @DeleteMapping("/api/types/cask/{typeName}")
   fun deleteCaskByTypeName(@PathVariable("typeName") typeName: String, @RequestParam(defaultValue = "false", required = false) force: Boolean = false): Mono<String>

   /**
    * Clears the contents of the cask for the given fully qualified named type.
    * If the cask is a view cask then it is a no-op.
    *
    * Returns a list of the name of casks that were cleared
    */
   @DeleteMapping("/api/{typeName}/contents")
   fun clearCaskByTypeName(@PathVariable("typeName") typeName: String): Mono<List<String>>

    @PutMapping("/api/casks/{typeName}/evictSchedule", produces = ["application/json"])
    fun setEvictionSchedule(@PathVariable("typeName") typeName: String, @RequestBody parameters: EvictionScheduleParameters): Mono<String>

    @PostMapping("/api/casks/{typeName}/evict", produces = ["application/json"])
    fun evict(@PathVariable("typeName") typeName: String, @RequestBody parameters: EvictionParameters): Mono<String>

}
