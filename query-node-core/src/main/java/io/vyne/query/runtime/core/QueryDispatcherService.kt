package io.vyne.query.runtime.core

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.query.ResultMode
import io.vyne.security.VynePrivileges
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import lang.taxi.query.TaxiQLQueryString
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*

@ConditionalOnBean(QueryDispatcher::class)
@RestController
class QueryDispatcherService(
   val objectMapper: ObjectMapper,
   private val queryDispatcher: QueryDispatcher
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
      val resultFlow = queryDispatcher!!.dispatchQuery(
         query,
         clientQueryId ?: UUID.randomUUID().toString(),
         mediaType = contentType
      ).asFlow()

      return ResponseEntity.status(HttpStatus.OK)
         .body(resultFlow)

   }
}
