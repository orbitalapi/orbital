package com.orbitalhq.query.runtime.core.dispatcher

import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.query.ResultMode
import com.orbitalhq.query.runtime.core.dispatcher.http.HttpQueryDispatcher
import com.orbitalhq.security.VynePrivileges
import com.orbitalhq.spring.http.BadRequestException
import com.orbitalhq.utils.withQueryId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.reactive.asFlow
import lang.taxi.query.TaxiQLQueryString
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*

/**
 * REST service that accepts a query.
 * The query isn't executed on this query node, but handed off to a query dispatcher
 * to be routed to a remote query execution node.
 */
@RestController
class QueryDispatcherService(
   val objectMapper: ObjectMapper,
   private val queryDispatcher: StreamingQueryDispatcher?
) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   init {
      logger.info { "Query dispatcher service is active" }
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.RunQuery}')")
   @PostMapping(
      value = ["/api/router/taxiql"],
      consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE, "application/taxiql"],
      produces = [MediaType.APPLICATION_JSON_VALUE]
   )
   suspend fun submitVyneQlQuery(
      @RequestBody query: TaxiQLQueryString,
      @RequestParam("resultMode", defaultValue = "RAW") resultMode: ResultMode,
      @RequestHeader(
         value = "Accept",
         defaultValue = MediaType.APPLICATION_JSON_VALUE
      ) contentType: String,
      auth: Authentication?,
      @RequestParam("clientQueryId", required = false) clientQueryId: String?
   ): ResponseEntity<Flow<Any>> {
      if (queryDispatcher == null) {
         logger.warn { "Received query to router, but no executor service has been enabled.  Returning bad request" }
         throw BadRequestException("Unable to execute the request at this time, due to configuration error.")
      }

      val actualClientId = clientQueryId ?: UUID.randomUUID().toString()
      val resultFlow = queryDispatcher.dispatchQuery(
         query,
         actualClientId,
         mediaType = contentType
      )
         .asFlow()
         .onCompletion { throwable ->
            if (throwable == null) {
               logger.withQueryId(actualClientId).debug { "Query $actualClientId completed" }
            } else {
               logger.withQueryId(actualClientId).info(throwable) { "Query $actualClientId failed" }
            }

         }
      return ResponseEntity.status(HttpStatus.OK)
         .body(resultFlow)

   }
}

