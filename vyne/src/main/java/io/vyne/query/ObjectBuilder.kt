package io.vyne.query

import io.vyne.models.MixedSources
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.models.TypedObject
import io.vyne.models.TypedValue
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Field
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Type
import lang.taxi.types.ObjectType

class ObjectBuilder(val queryEngine: QueryEngine, val context: QueryContext) {

   fun build(targetType: QualifiedName): TypedInstance? {
      return build(context.schema.type(targetType))
   }

   fun build(targetType: Type): TypedInstance? {
      val nullableFact = context.getFactOrNull(targetType, FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY)
      if (nullableFact != null) {
         val instance = nullableFact as TypedCollection
         when (instance.size) {
            0 -> error("Found 0 instances of ${targetType.fullyQualifiedName}, but hasFactOfType returned true")
            1 -> {
               val discoveredValue = instance.first()
               // Handle formatting
               // Choosing to copy the type, since as we got this far, we know that
               // the types are compatible.  However, this may prove to cause problems.
               return convertValue(discoveredValue, targetType)
            }
            else -> error("Found ${instance.size} instances of ${targetType.fullyQualifiedName}.  We should handle this, but we don't.")
         }
      }

      return if (targetType.isScalar) {
         findScalarInstance(targetType)
      } else {
         buildObjectInstance(targetType)
      }
   }

   private fun convertValue(discoveredValue: TypedInstance, targetType: Type): TypedInstance {
      return if (discoveredValue is TypedValue && targetType.hasFormat && targetType.format != discoveredValue.type.format) {
         discoveredValue.copy(targetType)
      } else {
         discoveredValue
      }
   }

   private fun buildObjectInstance(targetType: Type): TypedInstance? {
      val populatedValues = mutableMapOf<String, TypedInstance>()
      val missingAttributes = mutableMapOf<AttributeName, Field>()
      // =============================================================
      // TODO think how to fix it properly
      // Quick and dirty fix for projection of ObjectType to another ObjectType
      // If source and target types are ObjectTypes, just copy properties in one iteration
      // With this fix projection time of 1000 items was reduced from 37seconds to 650ms!
      // Enum filtering will be removed once the updated enum processing logic branch is merged.
      if (targetType.taxiType is ObjectType && context.facts.filter { !it.type.isEnum }.size == 1) {
         val sourceObjectType = context.facts.filter { !it.type.isEnum }.iterator().next()
         if (sourceObjectType is TypedObject) {
            targetType.attributes.forEach { (attributeName, field) ->
               val targetAttributeType = context.schema.type(field.type)
               val returnTypedNull = true
               when (val value = sourceObjectType.getAttributeIdentifiedByType(targetAttributeType, returnTypedNull)) {
                  is TypedNull -> missingAttributes[attributeName] = field
                  else -> populatedValues[attributeName] = convertValue(value, targetAttributeType)
               }
            }
         }
      } else {
         targetType.attributes.forEach { (attributeName, field) ->
            missingAttributes[attributeName] = field
         }
      }

      missingAttributes.forEach { (attributeName, field) ->
         val value = build(field.type)
         if (value != null) {
            populatedValues[attributeName] = value
         }
      }

      return TypedObject(targetType, populatedValues, MixedSources)

   }

   private fun findScalarInstance(targetType: Type): TypedInstance? {
      // Try searching for it.
      //log().debug("Trying to find instance of ${targetType.fullyQualifiedName}")
      val result = queryEngine.find(targetType, context)
      return if (result.isFullyResolved) {
         result[targetType] ?: error("Expected result to contain a ${targetType.fullyQualifiedName} ")
      } else {
         null
      }
   }
}
