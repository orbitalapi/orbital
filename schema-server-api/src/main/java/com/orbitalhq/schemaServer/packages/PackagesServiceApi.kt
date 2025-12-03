package com.orbitalhq.schemaServer.packages

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.PackageMetadata
import com.orbitalhq.ParsedPackage
import com.orbitalhq.config.ConfigSourceErrorMessage
import com.orbitalhq.schema.publisher.PublisherHealth
import com.orbitalhq.schema.publisher.PublisherType
import java.time.Instant

data class SourcePackageDescription(
   val identifier: PackageIdentifier,
   val health: PublisherHealth,
   val sourceCount: Int,
   val warningCount: Int,
   val errorCount: Int,
   val publisherType: PublisherType,
   val editable: Boolean,
   val submissionDate: Instant,
   val packageConfig: Any?, // GitRepositoryConfig or  FileSystemPackageSpec
   val configurationFileErrors: Map<String,ConfigSourceErrorMessage>
) {
   val uriPath: String = PackageIdentifier.toUriSafeId(identifier)
}

data class PackageWithDescription(
   val parsedPackage: ParsedPackage,
   val description: SourcePackageDescription
) {
   companion object {
      // for testing
      fun empty(packageIdentifier: PackageIdentifier): PackageWithDescription {
         return PackageWithDescription(
            ParsedPackage(
               PackageMetadata.from(packageIdentifier),
               emptyList(),
               emptyMap(),
               readme = null
            ),
            SourcePackageDescription(
               packageIdentifier,
               PublisherHealth(PublisherHealth.Status.Healthy),
               0,
               0,
               0,
               PublisherType.FileSystem,
               true,
               Instant.now(),
               null,
               emptyMap()
            )
         )

      }
   }
}
