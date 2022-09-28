package io.vyne.schemas

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import lang.taxi.utils.quotedIfNecessary


// TODO : Rename to annotations, to align with Taxi concept

data class Metadata(
   val name: QualifiedName, val params: Map<String, Any?> = emptyMap()
) {
   fun asTaxi(): String {
      val paramsList = params.map { (key, value) ->
         val valueText = if (value == null) {
            ""
         } else {
            " = " + value.quotedIfNecessary()
         }
         "$key$valueText"
      }
      val paramsCode = if (paramsList.isEmpty()) {
         ""
      } else {
         "(${paramsList.joinToString(", ")})"
      }
      return "@${name.fullyQualifiedName}$paramsCode"
   }
}

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
