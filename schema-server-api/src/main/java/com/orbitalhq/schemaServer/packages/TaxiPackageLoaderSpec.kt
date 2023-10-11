package com.orbitalhq.schemaServer.packages

import com.orbitalhq.schemaServer.packages.PackageLoaderSpec
import com.orbitalhq.schemaServer.packages.PackageType

/**
 * Placeholder, since all the metadata info is available in the spec we load
 * from the source.
 */
object TaxiPackageLoaderSpec : PackageLoaderSpec {
   override val packageType: PackageType = PackageType.Taxi
}

