package io.vyne.schemas

import com.google.common.annotations.Beta
import com.google.common.hash.Hashing
import io.vyne.SchemaId
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
   val fullyQualifiedName = taxiType.qualifiedName
   val versionHash: String
   val versionedNameHash: String
   val versionedName: String

   val sourceSchemaIds:List<SchemaId> by lazy {
      sources.map { it.id }
   }
   val sourceContent:List<String> by lazy {
      sources.map { it.content }
   }

   init {
      val qualifiedNameHash = Hashing.sha256().hashString(fullyQualifiedName, Charset.defaultCharset()).toString()
         .substring(0, 6)

      // If the fully qualified name is shorter than the hashed version - just use that.
      val nameToHash = listOf(fullyQualifiedName, qualifiedNameHash).minByOrNull { it.length }!!
      versionHash = taxiType.definitionHash!!
      versionedNameHash = "${nameToHash}_$versionHash"
      versionedName = "${fullyQualifiedName}@$versionHash"
   }

   override fun toString(): String {
      return versionedName
   }

}
