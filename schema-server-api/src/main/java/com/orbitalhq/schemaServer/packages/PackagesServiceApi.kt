package com.orbitalhq.schemaServer.packages

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.PackageMetadata
import com.orbitalhq.ParsedPackage
import com.orbitalhq.schema.publisher.PublisherHealth
import com.orbitalhq.schema.publisher.PublisherType

data class SourcePackageDescription(
   val identifier: PackageIdentifier,
   val health: PublisherHealth,
   val sourceCount: Int,
   val warningCount: Int,
   val errorCount: Int,
   val publisherType: PublisherType,
   val editable: Boolean,
   val packageConfig: Any? // GitRepositoryConfig or  FileSystemPackageSpec
   // TODO : Other things for visualisation
) {
   val uriPath: String = PackageIdentifier.toUriSafeId(identifier)

   companion object {
   }

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
               emptyMap()
            ),
            SourcePackageDescription(
               packageIdentifier,
               PublisherHealth(PublisherHealth.Status.Healthy),
               0,
               0,
               0,
               PublisherType.FileSystem,
               true,
               null
            )
         )

      }
   }
}
