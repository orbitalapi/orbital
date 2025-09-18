package com.orbitalhq.functions.loaders

import com.orbitalhq.PackageMetadata
import com.orbitalhq.VersionedSource
import com.orbitalhq.functions.TaxiFunctionProvider

/**
 * Responsible for loading and returning the classes that
 * expose TaxiFunctionProvider instances.
 */
interface CustomFunctionProvider {
   /**
    * Returns classes that a
    */
   fun loadFunctionClasses(
      sourceFiles: List<VersionedSource>,
      packageMetadata: PackageMetadata
   ): List<TaxiFunctionProvider>
}

// For testing
class SimpleFunctionClassProvider(private val providers: List<TaxiFunctionProvider>) : CustomFunctionProvider {
   override fun loadFunctionClasses(
      sourceFiles: List<VersionedSource>,
      packageMetadata: PackageMetadata
   ): List<TaxiFunctionProvider> {
      return providers
   }

}


