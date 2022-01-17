package io.vyne.schemaConsumerApi

import io.vyne.schemaApi.SchemaSet
import io.vyne.schemas.SchemaSetChangedEvent
import org.reactivestreams.Publisher

/**
 * Responsible for storing the retrieved schemas
 * and storing locally for usage
 */
interface SchemaStore: SchemaChangedEventProvider {
   fun schemaSet(): SchemaSet
   val generation: Int

}

interface SchemaChangedEventProvider {
   val schemaChanged: Publisher<SchemaSetChangedEvent>
}
