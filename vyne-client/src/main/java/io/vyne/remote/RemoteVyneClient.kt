package io.vyne.remote

import io.vyne.VyneClient
import io.vyne.VyneClientWithSchema
import io.vyne.schema.consumer.SchemaStore
import io.vyne.schemas.Schema
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
}


class RemoteVyneClientWithSchema(queryService: RemoteVyneQueryService, private val schemaStore: SchemaStore) :
   VyneClientWithSchema, RemoteVyneClient(queryService) {
   override val schema: Schema
      get() = schemaStore.schemaSet.schema

   override fun <T : Any> queryWithType(query: String, type: Class<T>): Flux<T> {
      return queryService.queryWithType(query, type, schema)
   }
}
