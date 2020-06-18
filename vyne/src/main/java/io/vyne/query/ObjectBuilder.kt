package io.vyne.query

import io.vyne.models.*
import io.vyne.schemas.*
import io.vyne.utils.log
import lang.taxi.types.ObjectType

class ObjectBuilder(val queryEngine: QueryEngine, val context: QueryContext) {

   fun build(targetType: QualifiedName): TypedInstance? {
      return build(context.schema.type(targetType))
   }

   fun build(targetType: Type): TypedInstance? {
      if (context.hasFactOfType(targetType, FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY)) {
         val instance = context.getFact(targetType, FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY) as TypedCollection
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
      // =============================================================
      // TODO think how to fix it properly
      // Quick and dirty fix for projection of ObjectType to another ObjectType
      // If source and target types are ObjectTypes, just copy properties in one iteration
      // With this fix projection time of 1000 items was reduced from 37seconds to 650ms!
      if (targetType.taxiType is ObjectType && context.facts.size == 1) {
         val sourceObjectType = context.facts.iterator().next()
         if (sourceObjectType is TypedObject) {
            val mappedValues: Map<String, TypedInstance> = targetType.attributes.mapNotNull { (attributeName, field) ->
               val targetAttributeType = context.schema.type(field.type)
               val returnTypedNull = true
               val value = sourceObjectType.getAttributeIdentifiedByType(targetAttributeType, returnTypedNull)
               when (value) {
                  is TypedNull -> build(field.type)?.let { attributeName to it } ?: null
                  is TypedInstance -> attributeName to convertValue(value, targetAttributeType)
                  else -> null
               }

            }.toMap()
            return TypedObject(targetType, mappedValues)
         }
      }
      // =============================================================

      // This is expensive, for each item in result and for each attribute in the item it performs
      // lots of deep recursive operations and creates lots of garbage objects!
      val missingAttributes = mutableMapOf<AttributeName, Field>()
      val mappedValues = targetType.attributes.mapNotNull { (attributeName, field) ->
         val value = build(field.type)
         if (value == null) {
            missingAttributes[attributeName] = field
            null
         } else {
            attributeName to value
         }
         // TODO : We need to improve support for nullable types here.
         // It's possible it's legit that the value wasn't found,
         // but that we can still build an instance of the object

      }.toMap()
      if (missingAttributes.isNotEmpty()) {
         log().debug("Couldn't build instance of ${targetType.fullyQualifiedName} as the following attributes weren't found: \n ${missingAttributes.map { (name, field) -> "$name : ${field.type.fullyQualifiedName}" }.joinToString("\n")}")
      }
      return TypedObject.fromAttributes(targetType, mappedValues, context.schema)
   }

   private fun findScalarInstance(targetType: Type): TypedInstance? {
      // Try searching for it.
      //log().debug("Trying to find instance of ${targetType.fullyQualifiedName}")
      val result = queryEngine.find(targetType, context)
      return if (result.isFullyResolved) {
         result[targetType] ?: error("Expected result to contain a ${targetType.fullyQualifiedName} ")
      } else {
         null;
      }
   }
}
