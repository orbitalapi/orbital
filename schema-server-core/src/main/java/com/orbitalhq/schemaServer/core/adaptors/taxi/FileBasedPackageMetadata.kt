package com.orbitalhq.schemaServer.core.adaptors.taxi

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.PackageMetadata
import java.nio.file.Path
import java.time.Instant

data class FileBasedPackageMetadata(
   override val identifier: PackageIdentifier,

   /**
    * The date that this packageMetadata was considered 'as-of'.
    * In the case that two packages with the same identifier are submitted,
    * the "latest" wins - using this data to determine latest.
    */
   override val submissionDate: Instant = Instant.now(),
   override val dependencies: List<PackageIdentifier> = emptyList(),
   val rootPath: Path
) : PackageMetadata