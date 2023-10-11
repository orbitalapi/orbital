package com.orbitalhq.schemaServer.packages

import com.orbitalhq.PackageIdentifier

data class SoapPackageLoaderSpec(
   val identifier: PackageIdentifier,
) : PackageLoaderSpec {
   override val packageType: PackageType = PackageType.Soap

}
