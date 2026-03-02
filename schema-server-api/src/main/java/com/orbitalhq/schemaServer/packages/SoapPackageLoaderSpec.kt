package com.orbitalhq.schemaServer.packages

import com.orbitalhq.PackageIdentifier
import lang.taxi.packages.CompilerOptions

data class SoapPackageLoaderSpec(
   val identifier: PackageIdentifier,
   val compilerOptions: CompilerOptions = CompilerOptions.DEFAULT
) : PackageLoaderSpec {
   override val packageType: PackageType = PackageType.Soap

}
