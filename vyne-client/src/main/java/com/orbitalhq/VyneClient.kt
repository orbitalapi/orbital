package com.orbitalhq

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.Schema
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery
import reactor.core.publisher.Flux

inline fun <reified T : Any> VyneClient.query(query: String): Flux<T> {
   return queryWithType(query, T::class.java)
}

interface VyneClient {
   fun <T : Any> queryWithType(query: String, type: Class<T>): Flux<T>

   fun queryAsTypedInstance(query: TaxiQLQueryString): Flux<TypedInstance>

   fun compile(query: TaxiQLQueryString): TaxiQlQuery
}

interface VyneClientWithSchema : VyneClient {
   val schema: Schema
}
