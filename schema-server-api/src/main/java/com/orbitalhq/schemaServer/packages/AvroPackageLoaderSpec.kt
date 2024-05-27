package com.orbitalhq.schemaServer.packages

import com.orbitalhq.PackageIdentifier

class AvroPackageLoaderSpec(
   val identifier: PackageIdentifier,
): PackageLoaderSpec {
   override val packageType: PackageType = PackageType.Avro
}
