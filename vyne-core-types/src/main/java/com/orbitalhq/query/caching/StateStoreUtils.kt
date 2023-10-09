package com.orbitalhq.query.caching

import com.orbitalhq.models.MixedSources
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.TypedObject
import com.orbitalhq.schemas.AttributeName
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.fqn
import lang.taxi.types.Field
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

// tested via HazelcastStateStoreProviderTest (for now)
fun calculateStateStoreId(instance: TypedInstance, schema: Schema): String? {
   if (instance !is TypedObject) {
      logger.warn { "Attempted to calculate the state store id of a non-object instance : ${instance.type.paramaterizedName}" }
      return null
   }
   val fields = getIdFields(instance.type)
   if (fields.isEmpty()) {
      return null
   }
   val idValues = fields.toList()
      .sortedBy { it.first }
      .map { (attributeName, field) ->
         val fieldType = field.resolveType(schema)
         val attributeValue = instance.getAttributeIdentifiedByType(fieldType)
         attributeValue.value?.toString()?.let { "${fieldType.qualifiedName}:$it" }
      }.distinct()

   return if (idValues.any { it == null }) {
      null
   } else {
      idValues.joinToString("|") { it!! }
   }
}

fun getIdFields(type: Type): Map<AttributeName, com.orbitalhq.schemas.Field> {
   return type.getAttributesWithAnnotation("Id".fqn())
}


fun mergeNonNullValues(oldValue: TypedInstance, newValue: TypedInstance, schema: Schema): TypedInstance {
   require(oldValue.type.paramaterizedName == newValue.type.paramaterizedName) { "Cannot merge instances, as they are of different types : old: ${oldValue.type.paramaterizedName} and new: ${newValue.type.paramaterizedName}" }
   require(oldValue is TypedObject) { "Merging is only supported on TypedObjects - got a ${oldValue::class.simpleName}" }
   require(newValue is TypedObject) { "Merging is only supported on TypedObjects - got a ${newValue::class.simpleName}" }
   val mergedValues = oldValue.type.attributes.map { (attributeName, _) ->
      val newValueAttributeValue = newValue[attributeName]
      val oldValueAttributeValue = oldValue[attributeName]
      // TODO : This should recurse for things that aren't TypedValue (ie, non-scalar values)
      val mergedAttributeValue = if (newValueAttributeValue is TypedNull) {
         oldValueAttributeValue
      } else {
         newValueAttributeValue
      }
      attributeName to mergedAttributeValue
   }.toMap()
   return TypedInstance.from(
      newValue.type,
      mergedValues,
      schema,
      source = MixedSources.singleSourceOrMixedSources(listOf(oldValue, newValue))
   )
}
