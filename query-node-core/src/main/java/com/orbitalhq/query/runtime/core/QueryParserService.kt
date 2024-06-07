package com.orbitalhq.query.runtime.core

import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schemas.ParsedQuery
import com.orbitalhq.schemas.SavedQuery
import com.orbitalhq.schemas.taxi.asSavedQuery
import lang.taxi.CompilationError
import lang.taxi.CompilationException
import lang.taxi.CompilationMessage
import lang.taxi.errors
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@RestController
class QueryParserService(private val schemaStore: SchemaStore) {

   @PostMapping(
      value = ["/api/taxiql/parse"],
      consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE, "application/taxiql"],
      produces = [MediaType.APPLICATION_JSON_VALUE]
   )
   fun parseQuery(
      @RequestBody query: TaxiQLQueryString,
   ): Mono<ParsedQuery> {
      return Mono.fromCallable {
         val schema = schemaStore.schema()
         try {
            val (compiledQuery, _, _) = schema.parseQuery(query)
            ParsedQuery(
               compiledQuery,
               emptyList()
            )
         } catch (e: CompilationException) {
            ParsedQuery(
               taxi = query,
               messages = e.errors,
               queryKind = null,
               name = null
            )
         }
      }.subscribeOn(Schedulers.boundedElastic())
   }
}

