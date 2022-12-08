package io.vyne.embedded

import io.vyne.VyneClient
import io.vyne.VyneClientWithSchema
import io.vyne.VyneProvider
import io.vyne.models.TypedInstance
import io.vyne.query.QueryContext
import io.vyne.query.QueryContextEventBroker
import io.vyne.schema.consumer.SchemaStore
import io.vyne.schemas.Schema
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.runBlocking
import reactor.core.publisher.Flux
import java.util.*

/**
 * An implementation of the VyneClient that uses an embedded Vyne instance instead of a remote one. Runs the queries
 * in the same JVM as the client.
 */
open class EmbeddedVyneClient(
   private val vyneProvider: VyneProvider
) : VyneClient {
   override fun <T : Any> queryWithType(query: String, type: Class<T>): Flux<T> {
      return runBlocking {
         return@runBlocking if (type == TypedInstance::class.java) {
            val flow = vyneProvider.createVyne().query(query).results as Flow<T>
            flow.asFlux()
         } else {
            val flow = vyneProvider.createVyne().query(query).rawResults as Flow<T>
            flow.asFlux()
         }
      }
   }

   fun from(
      facts: Set<TypedInstance>,
      queryId: String = UUID.randomUUID().toString(),
      clientQueryId: String? = null,
      eventBroker: QueryContextEventBroker = QueryContextEventBroker()
   ): QueryContext {
      return vyneProvider.createVyne().from(
         facts,
         queryId,
         clientQueryId,
         eventBroker
      )
   }

   fun from(
      fact: TypedInstance,
      queryId: String = UUID.randomUUID().toString(),
      clientQueryId: String? = null,
      eventBroker: QueryContextEventBroker = QueryContextEventBroker()
   ): QueryContext {
      return vyneProvider.createVyne().from(
         fact,
         queryId,
         clientQueryId,
         eventBroker
      )
   }
}

class EmbeddedVyneClientWithSchema(vyneProvider: VyneProvider, private val schemaStore: SchemaStore) :
   VyneClientWithSchema, EmbeddedVyneClient(vyneProvider) {
   override val schema: Schema
      get() = schemaStore.schemaSet.schema
}
