package com.orbitalhq.cockpit.core.query

import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.query.QueryParseMetadata
import com.orbitalhq.spring.http.BadRequestException
import lang.taxi.query.TaxiQLQueryString
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class QueryParserService(
   private val schemaStore: SchemaStore,
) {
   private val insightUtils = QueryInsightUtils()

   @PostMapping(
      value = ["/api/taxiql/parse"],
      consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE, "application/taxiql"],
      produces = [MediaType.APPLICATION_JSON_VALUE]
   )
   fun parseQuery(
      @RequestBody query: TaxiQLQueryString,
   ): Mono<QueryParseMetadata> {
      if (query.isEmpty()) throw BadRequestException("No query was provided")
      val schema = schemaStore.schema()
      return insightUtils.parseQuery(query, schema)
   }
}

