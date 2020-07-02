package io.vyne.queryService

import io.vyne.cask.api.CaskApi
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.RestController

/**
 * This class provides simple proxying to Cask
 */
@RestController
class CaskService(private val caskApi: CaskApi): CaskApi {

   override fun ingestMessage(contentType: String, typeReference: String, queryParams: MultiValueMap<String, String?>, input: String) = caskApi.ingestMessage(contentType, typeReference, queryParams, input)
   override fun getCasks() = caskApi.getCasks()
   override fun getCaskDetails(tableName: String) =  caskApi.getCaskDetails(tableName)
   override fun deleteCask(tableName: String) = caskApi.deleteCask(tableName)
   override fun emptyCask(tableName: String) = caskApi.emptyCask(tableName)
}
