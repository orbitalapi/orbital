package io.vyne.schemaServer.core.adaptors

import com.fasterxml.jackson.annotation.JsonProperty
import io.vyne.schema.api.PackageIdentifier
import java.net.URI
import java.time.Instant

data class OpenApiPackageLoaderSpec(
   val identifier: PackageIdentifier,

   val uri: URI,
   val defaultNamespace: String,

   /**
    * The date that this packageMetadata was considered 'as-of'.
    * In the case that two packages with the same identifier are submitted,
    * the "latest" wins - using this data to determine latest.
    *
    * However, we don't persist this value back out
    */
   @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
   val submissionDate: Instant = Instant.now(),
   val dependencies: List<PackageIdentifier> = emptyList(),
) : PackageLoaderSpec {
   override val packageType: PackageType = PackageType.OpenApi
}
