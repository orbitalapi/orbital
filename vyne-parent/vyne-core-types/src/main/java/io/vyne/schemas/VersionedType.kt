package io.vyne.schemas

import com.google.common.annotations.Beta
import com.google.common.hash.Hashing
import io.vyne.VersionedSource
import java.nio.charset.Charset


/**
 * Indciates a type, along with the sources that defined it.
 * The idea here is that if the sources change, the type may have a different
 * definition
 */
@Beta
data class VersionedType(
   val sources: List<VersionedSource>,
   // Migrating away from Vyne Type to taxiType
   @Deprecated("use taxiType instead")
   val type: Type,
   val taxiType: lang.taxi.types.Type
) {
   val fullyQualifiedName = type.fullyQualifiedName
   val versionHash: String
   val versionedNameHash: String

   val versionedName: String

   init {
      val sourceIds = sources.map { it.id }
         .sorted()
         .joinToString("|")
      val qualifiedNameHash = Hashing.sha256().hashString(fullyQualifiedName, Charset.defaultCharset()).toString()
         .substring(0, 6)

      // If the fully qualified name is shorter than the hashed version - just use that.
      val nameToHash = listOf(type.fullyQualifiedName, qualifiedNameHash).minBy { it.length }!!
      versionHash = Hashing.sha256().hashString(sourceIds, Charset.defaultCharset()).toString()
         .substring(0, 6)
      versionedNameHash = "${nameToHash}_$versionHash"
      versionedName = "${fullyQualifiedName}@$versionHash"
   }


}
