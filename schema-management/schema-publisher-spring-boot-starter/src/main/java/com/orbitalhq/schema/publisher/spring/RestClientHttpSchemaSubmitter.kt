package com.orbitalhq.schema.publisher.spring

import com.orbitalhq.SourcePackage
import com.orbitalhq.schema.publisher.SourceSubmissionResponse
import com.orbitalhq.schema.publisher.http.HttpSchemaSubmitter
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import reactor.core.publisher.Mono

/**
 * Implementation of HttpSchemaSubmitter using Spring's RestClient.
 *
 * This submitter sends schema packages to the Orbital Schema Server via HTTP.
 */
class RestClientHttpSchemaSubmitter(
   private val restClient: RestClient
) : HttpSchemaSubmitter {

   private val logger = KotlinLogging.logger {}

   /**
    * Submits a source package to the schema server.
    *
    * @param submission The source package containing schemas to publish
    * @return A Mono containing the submission response from the server
    */
   override fun submitSources(submission: SourcePackage): Mono<SourceSubmissionResponse> {
      logger.debug { "Submitting package ${submission.packageMetadata.identifier} with ${submission.sources.size} sources" }

      return try {
         val response = restClient.post()
            .uri("/api/schemas/taxi")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(submission)
            .retrieve()
            .body<SourceSubmissionResponse>()

         if (response != null) {
            logger.info { "Successfully submitted package ${submission.packageMetadata.identifier}" }
            Mono.just(response)


         } else {
            logger.warn { "Received null response when submitting package ${submission.packageMetadata.identifier}" }
            Mono.error(IllegalStateException("Schema server returned null response"))
         }
      } catch (e: Exception) {
         logger.warn { "Failed to submit package ${submission.packageMetadata.identifier} - ${e.message}" }
         Mono.error(e)
      }
   }
}
