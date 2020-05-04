package io.vyne

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.fqn
import lang.taxi.packages.PackageIdentifier

/**
 * Not an actual type, but a reference to a named type,
 * from a specific version of a published package
 */
@JsonSerialize(using = VersionedTypeReferenceSerializer::class)
@JsonDeserialize(using = VersionedTypeReferenceDeserializer::class)
data class VersionedTypeReference(
   val typeName: QualifiedName,
   val packageIdentifier: PackageIdentifier = PackageIdentifier.UNSPECIFIED
) {
   override fun toString(): String {
      val version = if (packageIdentifier != PackageIdentifier.UNSPECIFIED) {
         packageIdentifier.id
      } else {
         null
      }
      return listOfNotNull(version, typeName.parameterizedName).joinToString(":")
   }

   companion object {
      fun parse(id: String): VersionedTypeReference {
         return if (id.contains(":")) {
            val (packageId, typeName) = id.split(":")
            return VersionedTypeReference(
               typeName.fqn(),
               PackageIdentifier.fromId(packageId)
            )
         } else {
            VersionedTypeReference(
               id.fqn()
            )
         }

      }
   }
}


class VersionedTypeReferenceSerializer : JsonSerializer<VersionedTypeReference>() {
   override fun serialize(value: VersionedTypeReference, gen: JsonGenerator, serializers: SerializerProvider) {
      gen.writeString(value.toString())
   }
}

class VersionedTypeReferenceDeserializer : JsonDeserializer<VersionedTypeReference>() {
   override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): VersionedTypeReference {
      val stringValue = p.valueAsString
      return VersionedTypeReference.parse(stringValue)
   }

}
