package com.orbitalhq

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.MetricTags
import com.orbitalhq.schemas.Schema
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery
import reactor.core.publisher.Flux
import java.security.Principal
import com.orbitalhq.query.EmitMetrics

inline fun <reified T : Any> VyneClient.query(query: String, metricsTags: MetricTags = MetricTags.NONE, principal: Principal? = null, emitMetrics: EmitMetrics = EmitMetrics.None): Flux<T> {
   return queryWithType(query, T::class.java, metricsTags, principal, emitMetrics)
}

interface VyneClient {
   fun <T : Any> queryWithType(query: String, type: Class<T>, metricsTags: MetricTags = MetricTags.NONE, principal: Principal? = null, emitMetrics: EmitMetrics = EmitMetrics.None): Flux<T>

   fun queryAsTypedInstance(query: TaxiQLQueryString, metricsTags: MetricTags = MetricTags.NONE, principal: Principal? = null, emitMetrics: EmitMetrics = EmitMetrics.None): Flux<TypedInstance>

   fun compile(query: TaxiQLQueryString): TaxiQlQuery


}

interface VyneClientWithSchema : VyneClient {
   val schema: Schema
}
