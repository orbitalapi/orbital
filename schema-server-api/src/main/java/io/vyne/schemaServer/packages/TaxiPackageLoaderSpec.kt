package io.vyne.schemaServer.packages

import io.vyne.schemaServer.packages.PackageLoaderSpec
import io.vyne.schemaServer.packages.PackageType

/**
 * Placeholder, since all the metadata info is available in the spec we load
 * from the source.
 */
object TaxiPackageLoaderSpec : PackageLoaderSpec {
   override val packageType: PackageType = PackageType.Taxi
}

