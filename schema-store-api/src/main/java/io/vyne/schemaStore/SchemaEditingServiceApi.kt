package io.vyne.schemaStore

import java.time.Instant

interface SchemaEditingServiceApi {
}

data class ResourceEditingResponse(
   val newVersion: String,
   val timestamp: Instant,
   val repositoryName: String,
   val resourcePath: String
)
