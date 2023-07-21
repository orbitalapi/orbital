package io.vyne.schemaServer.packages

import io.vyne.PackageIdentifier

data class SoapPackageLoaderSpec(
   val identifier: PackageIdentifier,
) : PackageLoaderSpec {
   override val packageType: PackageType = PackageType.Soap

}
