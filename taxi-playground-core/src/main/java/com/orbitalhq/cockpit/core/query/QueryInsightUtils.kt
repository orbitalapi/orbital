package com.orbitalhq.cockpit.core.query

import com.orbitalhq.Message
import com.orbitalhq.query.QueryParseMetadata
import com.orbitalhq.query.QueryPlan
import com.orbitalhq.query.SearchFailedException
import com.orbitalhq.schemas.Schema
import lang.taxi.CompilationException
import lang.taxi.messages.Severity
import lang.taxi.query.TaxiQLQueryString
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

/**
 * Helpers which provide parsing utils for the UI.
 * Different from the QueryParser in the taxiql query engine,
 * which is part of the actual query building process
 */
class QueryInsightUtils() {
   private val visualizerService = QueryVisualizer()

   fun parseQuery(
      query: TaxiQLQueryString,
      schema: Schema,
   ): Mono<QueryParseMetadata> {
      require(query.isNotEmpty()) { "No query was provided"}
      return Mono.fromCallable {
         val (compiledQuery, querySchema) = try {
            val (compiledQuery, _, querySchema) = schema.parseQuery(query)
            compiledQuery to querySchema
         } catch (e: CompilationException) {
            return@fromCallable QueryParseMetadata.compilationFailed(
               taxi = query,
               errors = e.errors,
            )
         }
         val queryPlan = try {
            val queryPlanSteps = visualizerService.visualizeQuery(query, querySchema)
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
         QueryParseMetadata.fromQuery(
            compiledQuery,
            compilationMessages = emptyList(),
            queryPlan = queryPlan,
            schema = querySchema
         )
      }.subscribeOn(Schedulers.boundedElastic())
   }
}

