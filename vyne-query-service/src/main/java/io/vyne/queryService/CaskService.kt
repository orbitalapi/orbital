package io.vyne.queryService

import io.vyne.cask.api.CaskApi
import io.vyne.cask.api.CaskIngestionResponse
import org.springframework.web.bind.annotation.RestController


// Simply defers all cask operations back to the cask microservice, using the feign controller that's injected
@RestController
class CaskService(private val feignCaskApi: CaskApi) : CaskApi by feignCaskApi {

   // For some reason, deferring this to the delegate (ie., not overriding here)
   // means defaults aren't parsed.
   override fun ingestCsv(typeReference: String, delimiter: Char, firstRecordAsHeader: Boolean, nullValue: Set<String>, ignoreContentBefore: String?, debug: Boolean, input: String): CaskIngestionResponse {
      return feignCaskApi.ingestCsv(typeReference, delimiter, firstRecordAsHeader, nullValue, ignoreContentBefore, debug, input)
   }

   override fun ingestJson(typeReference: String, debug: Boolean, input: String): CaskIngestionResponse {
      return feignCaskApi.ingestJson(typeReference, debug, input)
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
