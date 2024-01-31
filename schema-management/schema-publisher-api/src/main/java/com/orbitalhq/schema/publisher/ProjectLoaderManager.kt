package com.orbitalhq.schema.publisher

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schema.publisher.loaders.SchemaPackageTransport

/**
 * Exposes loaders that can load (and sometimes update) projects over
 * various transports
 */
interface ProjectLoaderManager {

   fun getLoaderOrNull(packageIdentifier: PackageIdentifier): SchemaPackageTransport?
   fun getLoader(packageIdentifier: PackageIdentifier): SchemaPackageTransport

   val loaders: List<SchemaPackageTransport>
   val editableLoaders: List<SchemaPackageTransport>
}
