package io.vyne

import io.vyne.schemas.QualifiedName
import lang.taxi.packages.PackageIdentifier

/**
 * Not an actual type, but a reference to a named type,
 * from a specific version of a published package
 */
data class VersionedTypeReference(
   val typeName: QualifiedName,
   val packageIdentifier: PackageIdentifier = PackageIdentifier.UNSPECIFIED
)
