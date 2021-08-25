package io.vyne.models

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.VersionedType
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable


@JsonDeserialize(using = TypeNamedInstanceDeserializer::class)
@JsonInclude(JsonInclude.Include.ALWAYS)
@Serializable
data class TypeNamedInstance(
   val typeName: String,
   @Contextual
   val value: Any?,
   val dataSourceId: String?
) {
   constructor(typeName: QualifiedName, value: Any?, source: DataSource? = null) : this(
      typeName.fullyQualifiedName,
      value,
      source?.id
   )

   constructor(typeName: String, value: Any?, source: DataSource? = null) : this(
      typeName,
      value,
      source?.id
   )

   fun convertToRaw(): Any? {
      return convertToRaw(this.value)
   }

   private fun convertToRaw(valueToConvert: Any?): Any? {
      return when (valueToConvert) {
         null -> null
         is List<*> -> (valueToConvert as List<TypeNamedInstance>).map { it.convertToRaw() }
         is Map<*, *> -> {
            val valueMap = valueToConvert as Map<String, Any>
            valueMap.map { (key, value) -> key to convertToRaw(value) }
               .toMap()
         }
         is TypeNamedInstance -> convertToRaw(valueToConvert.value)
         else -> valueToConvert
      }
   }
}

data class VersionedTypedInstance(
   val type: VersionedType,
   val instance: TypedInstance
)
