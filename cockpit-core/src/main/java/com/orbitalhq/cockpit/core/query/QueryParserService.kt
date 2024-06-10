package com.orbitalhq.cockpit.core.query

import com.orbitalhq.Message
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.query.ParsedQuery
import com.orbitalhq.query.QueryPlan
import com.orbitalhq.query.SearchFailedException
import com.orbitalhq.query.history.QuerySankeyChartRow
import lang.taxi.CompilationException
import lang.taxi.messages.Severity
import lang.taxi.query.TaxiQLQueryString
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@RestController
class QueryParserService(
   private val schemaStore: SchemaStore,
) {
   private val visualizerService = QueryVisualizerService()

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
         val compiledQuery = try {
            val (compiledQuery, _, _) = schema.parseQuery(query)
            compiledQuery
         } catch (e: CompilationException) {
            return@fromCallable ParsedQuery.compilationFailed(
               taxi = query,
               errors = e.errors,
            )
         }
         val queryPlan = try {
            val queryPlanSteps = visualizerService.visualizeQuery(query, schema)
            QueryPlan(queryPlanSteps, emptyList())
         } catch (e: SearchFailedException) {
            QueryPlan(
               emptyList(), listOf(
                  Message(
                     Severity.ERROR, e.message ?: e::class.simpleName!!
                  )
               )
            )
         }
         ParsedQuery(
            compiledQuery,
            compilationMessages = emptyList(),
            queryPlan = queryPlan,
         )
      }.subscribeOn(Schedulers.boundedElastic())
   }
}

