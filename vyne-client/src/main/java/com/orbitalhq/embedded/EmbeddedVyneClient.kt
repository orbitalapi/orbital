package com.orbitalhq.embedded

import com.orbitalhq.VyneClient
import com.orbitalhq.VyneClientWithSchema
import com.orbitalhq.VyneProvider
import com.orbitalhq.auth.getAuthClaimsAsFacts
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.EmitMetrics
import com.orbitalhq.query.MetricTags
import com.orbitalhq.query.QueryContext
import com.orbitalhq.query.QueryContextEventBroker
import com.orbitalhq.query.QueryResult
import com.orbitalhq.query.tracing.NoopTracingEventSink
import com.orbitalhq.query.tracing.TraceContext
import com.orbitalhq.query.tracing.TracingEventSink
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schemas.Schema
import com.orbitalhq.utils.Ids
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.runBlocking
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery
import reactor.core.publisher.Flux
import reactor.kotlin.core.publisher.toFlux
import java.security.Principal
import java.util.*


/**
 * An implementation of the VyneClient that uses an embedded Vyne instance instead of a remote one. Runs the queries
 * in the same JVM as the client.
 */
open class EmbeddedVyneClient(
   private val vyneProvider: VyneProvider,
   private val tracingEventSink: TracingEventSink = NoopTracingEventSink
) : VyneClient {
   override fun <T : Any> queryWithType(
      query: String,
      type: Class<T>,
      metricsTags: MetricTags,
      principal: Principal?,
      emitMetrics: EmitMetrics,
      traceId: String
   ): Flux<T> {
      return runBlocking {
         val vyne = vyneProvider.createVyne()
         val authClaims = principal.getAuthClaimsAsFacts(vyne.schema)
         return@runBlocking if (type == TypedInstance::class.java) {
            val queryResult = vyneProvider.createVyne()
               .query(query, metricsTags = metricsTags, executionContextFacts = authClaims.toSet())
            captureMetrics(queryResult, emitMetrics)
            queryResult.results.asFlux() as Flux<T>
         } else {
            val queryId = Ids.fastUuid()
            val queryResult = vyneProvider.createVyne().query(
               query,
               metricsTags = metricsTags,
               executionContextFacts = authClaims.toSet(),
               queryId =  queryId,
               eventBroker = QueryContextEventBroker(traceSpan = TraceContext.forTraceId(traceId,queryId, tracingEventSink).rootSpan)
            )
            captureMetrics(queryResult, emitMetrics)
            (queryResult.rawResults as Flow<T>).asFlux()
         }
      }
   }

   private fun captureMetrics(queryResult: QueryResult, emitMetrics: EmitMetrics) {
      vyneProvider.emitMetrics(queryResult, emitMetrics)
   }

   override fun compile(query: TaxiQLQueryString): TaxiQlQuery {
      val (parseResult, _) = vyneProvider.createVyne().parseQuery(query)
      return parseResult
   }


   override fun queryAsTypedInstance(
      query: TaxiQLQueryString,
      metricsTags: MetricTags,
      principal: Principal?,
      emitMetrics: EmitMetrics,
      traceId: String
   ): Flux<TypedInstance> {
      // This is obviously not correct.
      // We're run blocking, and then wrapping a list to a flux, it's all sorts of level of messed up
      // But, it works. We REALLY need to get this async shit sorted out.
      val flow = runBlocking {
         vyneProvider.createVyne().query(query, metricsTags = metricsTags,traceId = traceId).results
            .toList()

      }
      return flow.toFlux()
   }

   fun from(
      facts: Set<TypedInstance>,
      queryId: String = UUID.randomUUID().toString(),
      clientQueryId: String? = null,
      traceContext: TraceContext,
      // TODO : I don't think this is used anymore. If it
      eventBroker: QueryContextEventBroker = QueryContextEventBroker(traceSpan = traceContext.rootSpan)
   ): QueryContext {
      return vyneProvider.createVyne().from(
         facts,
         queryId,
         clientQueryId,
         traceContext.traceId,
         eventBroker,
      )
   }

   fun from(
      fact: TypedInstance,
      queryId: String = UUID.randomUUID().toString(),
      clientQueryId: String? = null,
      traceContext: TraceContext,
      eventBroker: QueryContextEventBroker = QueryContextEventBroker(traceSpan = traceContext.rootSpan)
   ): QueryContext {
      return vyneProvider.createVyne().from(
         fact,
         queryId,
         clientQueryId,
         traceContext.traceId,
         eventBroker
      )
   }
}

class EmbeddedVyneClientWithSchema(vyneProvider: VyneProvider, private val schemaStore: SchemaStore) :
   VyneClientWithSchema, EmbeddedVyneClient(vyneProvider) {
   override val schema: Schema
      get() {
         return schemaStore.schemaSet.schema
      }

}
