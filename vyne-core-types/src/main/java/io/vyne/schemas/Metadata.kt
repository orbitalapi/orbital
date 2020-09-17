package io.vyne.schemas

// TODO : Rename to annotations, to align with Taxi concept
data class Metadata(val name: QualifiedName, val params: Map<String, Any?> = emptyMap())

interface MetadataTarget {
   val metadata: List<Metadata>
   fun metadata(name: String): Metadata {
      return metadata.firstOrNull { it.name.fullyQualifiedName == name }
         ?: throw IllegalArgumentException("$name not present within this metadata")
   }

   fun hasMetadata(name: String): Boolean {
      return this.metadata.any { it.name.fullyQualifiedName == name }
   }
}
