package io.vyne.schemas

import io.vyne.schemaStore.SchemaSet
import reactor.core.publisher.Mono

// This uses a mono, rather than the actual schema set, in case no-one is listener, and therefore
// we don't need to incur the deferred cost of calcualting the new schemaset
data class SchemaSetChangedEvent(val oldSchemaSet: SchemaSet?, val newSchemaSet: SchemaSet)
