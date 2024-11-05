package com.orbitalhq.schemas

import lang.taxi.utils.quotedIfNecessary


// TODO : Remove, and just use Taxi's annotation class.
data class Metadata(
   val name: QualifiedName,
   val params: Map<String, Any?> = emptyMap(),
) {
   companion object {
      fun getVariableName(key: String): String? {
         return when {
            key.startsWith("\${") && key.endsWith("}") -> { key.substring(2, key.length - 1) }
            else -> null
         }
      }
   }
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
   fun firstMetadata(name: String): Metadata {
      return metadata.firstOrNull { it.name.fullyQualifiedName == name }
         ?: throw IllegalArgumentException("$name not present within this metadata")
   }
   fun allMetadata(name: String):List<Metadata> {
      return metadata.filter { it.name.fullyQualifiedName == name }
   }

   fun hasMetadata(name: String): Boolean {
      return metadata.hasMetadata(name)
   }
}

fun List<Metadata>.hasMetadata(name: String): Boolean {
   return this.any { it.name.fullyQualifiedName == name }
}
