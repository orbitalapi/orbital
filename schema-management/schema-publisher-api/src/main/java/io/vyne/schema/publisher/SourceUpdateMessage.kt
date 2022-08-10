package io.vyne.schema.publisher

import io.vyne.PackageIdentifier
import io.vyne.SourcePackage
import io.vyne.UnversionedPackageIdentifier

sealed class PackageDelta(
   val kind: PackageUpdateKind,
   val packageId: UnversionedPackageIdentifier,
)

enum class PackageUpdateKind {
   Added,
   Removed,
   Updated
}

data class PackageAdded(val newState: SourcePackage) :
   PackageDelta(PackageUpdateKind.Added, newState.identifier.unversionedId)

data class PackageRemoved(val oldStateId: PackageIdentifier, val oldState: SourcePackage? = null) :
   PackageDelta(PackageUpdateKind.Removed, oldStateId.unversionedId) {
   constructor(oldState: SourcePackage) : this(oldState.identifier, oldState)
}

data class PackageUpdated(val oldState: SourcePackage, val newState: SourcePackage) : PackageDelta(PackageUpdateKind.Updated, oldState.identifier.unversionedId) {
   init {
       require(oldState.identifier.unversionedId == newState.identifier.unversionedId) { "The unversionedIdentifier for both packages must be the same"}
   }
}
