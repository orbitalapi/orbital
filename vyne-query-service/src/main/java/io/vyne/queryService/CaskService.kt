package io.vyne.queryService

import io.vyne.cask.api.CaskApi
import io.vyne.cask.api.CaskIngestionResponse
import mu.KotlinLogging
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono


// Simply defers all cask operations back to the cask microservice, using the feign controller that's injected
@RestController
class CaskService(private val feignCaskApi: CaskApi) : CaskApi by feignCaskApi {
   private val logger = KotlinLogging.logger {}

   // For some reason, deferring this to the delegate (ie., not overriding here)
   // means defaults aren't parsed.
   override fun ingestCsv(typeReference: String, delimiter: Char, firstRecordAsHeader: Boolean, nullValue: Set<String>, ignoreContentBefore: String?,  containsTrailingDelimiters: Boolean , debug: Boolean, input: String): Mono<CaskIngestionResponse> {
      logger.info { "Uploading Csv for $typeReference" }
      return feignCaskApi.ingestCsv(typeReference, delimiter, firstRecordAsHeader, nullValue, ignoreContentBefore, debug, containsTrailingDelimiters, input)
   }

   override fun ingestJson(typeReference: String, debug: Boolean, input: String): Mono<CaskIngestionResponse> {
      logger.info { "Uploading json for $typeReference" }
      return feignCaskApi.ingestJson(typeReference, debug, input)
   }

   override fun ingestXml(typeReference: String, debug: Boolean, elementSelector: String?, input: String): Mono<CaskIngestionResponse> {
      logger.info { "Uploading xml for $typeReference" }
      return feignCaskApi.ingestXml(typeReference, debug, elementSelector, input)
   }

//
//   override fun ingestCsv(typeReference: String, parameters: CsvIngestionParameters, input: String) = caskApi.ingestCsv(typeReference, parameters, input)
//   override fun ingestJson(typeReference: String, parameters: JsonIngestionParameters, input: String) = caskApi.ingestJson(typeReference, parameters, input)
//
//   override fun getCasks() = caskApi.getCasks()
//   override fun getCaskDetails(tableName: String) = caskApi.getCaskDetails(tableName)
//   override fun deleteCask(tableName: String) = caskApi.deleteCask(tableName)
//   override fun emptyCask(tableName: String) = caskApi.emptyCask(tableName)
}
