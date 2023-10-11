package com.orbitalhq.remote

import com.orbitalhq.VyneClient
import com.orbitalhq.VyneClientWithSchema
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schemas.Schema
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery
import reactor.core.publisher.Flux

/**
 * An implementation of the VyneClient that uses a remote Vyne instance instead of an embedded one. Runs the queries
 * with the provided RemoteVyneQueryService implementation which allows users to configure how to execute the queries.
 */
open class RemoteVyneClient(
   protected val queryService: RemoteVyneQueryService
) : VyneClient {
   override fun <T : Any> queryWithType(query: String, type: Class<T>): Flux<T> {
      return queryService.queryWithType(query, type)
   }

   override fun queryAsTypedInstance(query: TaxiQLQueryString): Flux<TypedInstance> {
      TODO("Not implemented yet")
   }

   override fun compile(query: TaxiQLQueryString): TaxiQlQuery {
      TODO("Not yet implemented")
   }
}


class RemoteVyneClientWithSchema(queryService: RemoteVyneQueryService, private val schemaStore: SchemaStore) :
   VyneClientWithSchema, RemoteVyneClient(queryService) {
   override val schema: Schema
      get() = schemaStore.schemaSet.schema

   override fun <T : Any> queryWithType(query: String, type: Class<T>): Flux<T> {
      return queryService.queryWithType(query, type, schema)
   }
}
