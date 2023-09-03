package com.orbitalhq.schema.publisher

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.SourcePackage
import com.orbitalhq.UnversionedPackageIdentifier
import com.orbitalhq.schemas.Schema
import lang.taxi.CompilationError


// Slightly more broadly scoped than PackagesUpdatedMessage.
// Intended that we can emit a PackagesUpdatedMessage without neccessarily knowing all the
// details at creation time (eg., current schema state or set of errors)
data class SchemaUpdatedMessage(
   val currentPackages: List<SourcePackage>,
   val deltas: List<PackageDelta>,
   val schema: Schema,
   val oldSchema: Schema,
   val errors: List<CompilationError>
) {

   constructor(
      packageUpdates: PackagesUpdatedMessage,
      schema: Schema,
      oldSchema: Schema,
      errors: List<CompilationError> = emptyList()
   ) : this(packageUpdates.currentPackages, packageUpdates.deltas, schema, oldSchema, errors)

   val messageKind: PackageUpdateMessageKind = if (deltas.isEmpty()) {
      PackageUpdateMessageKind.StateOfTheWorld
   } else {
      PackageUpdateMessageKind.Delta
   }

}

data class PackagesUpdatedMessage(
   val currentPackages: List<SourcePackage>,
   val deltas: List<PackageDelta>,
) {

   /**
    * Schema Ids that were removed since the last status message
    */
   val removedSchemaIds: List<PackageIdentifier> = deltas
      .filterIsInstance<PackageRemoved>()
      .map { it.oldStateId }
}

enum class PackageUpdateMessageKind {
   Delta,
   StateOfTheWorld
}

sealed class PackageDelta(
   val kind: PackageUpdateKind,
   val packageId: UnversionedPackageIdentifier,
)

enum class PackageUpdateKind {
   Added,
   Removed,
   Updated,
   PublisherHealthChanged,
}

data class PackageAdded(val newState: SourcePackage) :
   PackageDelta(PackageUpdateKind.Added, newState.identifier.unversionedId)

data class PackageRemoved(val oldStateId: PackageIdentifier, val oldState: SourcePackage? = null) :
   PackageDelta(PackageUpdateKind.Removed, oldStateId.unversionedId) {
   constructor(oldState: SourcePackage) : this(oldState.identifier, oldState)
}

data class PackageUpdated(val oldState: SourcePackage, val newState: SourcePackage) :
   PackageDelta(PackageUpdateKind.Updated, oldState.identifier.unversionedId) {
   init {
      require(oldState.identifier.unversionedId == newState.identifier.unversionedId) { "The unversionedIdentifier for both packages must be the same" }
   }
}

data class PublisherHealthUpdated(val identifier: PackageIdentifier, val health: PublisherHealth) :
   PackageDelta(PackageUpdateKind.PublisherHealthChanged, identifier.unversionedId)