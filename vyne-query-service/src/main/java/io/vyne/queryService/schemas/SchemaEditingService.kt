package io.vyne.queryService.schemas

import io.vyne.schemaStore.ResourceEditingResponse
import io.vyne.utils.log
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange

@RestController
class SchemaEditingService(private val restTemplate: RestTemplate) {
   @PostMapping(value = ["/api/repository/{repositoryName}"])
   fun writeContentToRepository(
      @PathVariable("repositoryName") repositoryName: String,
      @RequestParam("resource") resourceName: String,
      @RequestBody fileContents: ByteArray,
      @RequestHeader(HttpHeaders.AUTHORIZATION) authHeader: String
   ): ResponseEntity<ResourceEditingResponse> {
      return sendToFileSchemaServer(
         url = "http://file-schema-server/api/repository/$repositoryName?resource=$resourceName",
         fileContents = fileContents,
         authHeader = authHeader
      )
   }

   @PostMapping(value = ["/api/repository"])
   fun writeContentToDefaultRepository(
      @RequestParam("resource") resourceName: String,
      @RequestBody fileContents: ByteArray,
      @RequestHeader(HttpHeaders.AUTHORIZATION) authHeader: String
   ): ResponseEntity<ResourceEditingResponse> {
      return sendToFileSchemaServer(
         url = "http://file-schema-server/api/repository?resource=$resourceName",
         fileContents = fileContents,
         authHeader = authHeader
      )
   }


   private fun sendToFileSchemaServer(
      url: String,
      fileContents: ByteArray,
      authHeader: String
   ): ResponseEntity<ResourceEditingResponse> {
      val headers = HttpHeaders()
      headers.set(HttpHeaders.AUTHORIZATION, authHeader)
      val request = HttpEntity(
         fileContents, headers
      )
      log().info("Attempting to publish repository update to $url")
      try {
         val response = restTemplate.exchange<ResourceEditingResponse>(
            url,
            HttpMethod.POST,
            request
         )
         log().info("Received ${response.statusCode} response from upstream service")
         return response
      } catch (exception: Exception) {
         log().error("Failed to write content upstream to file schema service - an exception occurred", exception)
         throw exception
      }
   }
}
