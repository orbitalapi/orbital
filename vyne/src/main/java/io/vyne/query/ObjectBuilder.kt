package io.vyne.query

import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.schemas.*
import io.vyne.utils.log

class ObjectBuilder(val queryEngine: QueryEngine, val context: QueryContext) {

   fun build(targetType: QualifiedName): TypedInstance? {
      return build(context.schema.type(targetType))
   }

   fun build(targetType: Type): TypedInstance? {
      if (context.hasFactOfType(targetType, FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY)) {
         val instance = context.getFact(targetType, FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY) as TypedCollection
         when (instance.size) {
            0 -> error("Found 0 instances of ${targetType.fullyQualifiedName}, but hasFactOfType returned true")
            1 -> return instance.first()
            else -> error("Found ${instance.size} instances of ${targetType.fullyQualifiedName}.  We should handle this, but we don't.")
         }
      }

      return if (targetType.isScalar) {
         findScalarInstance(targetType)
      } else {
         buildObjectInstance(targetType)
      }
   }

   private fun buildObjectInstance(targetType: Type): TypedInstance? {
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
         log().warn("Couldn't build instance of ${targetType.fullyQualifiedName} as the following attributes weren't found: \n ${missingAttributes.map { (name, field) -> "$name : ${field.type.fullyQualifiedName}" }.joinToString("\n")}")
      }
      return TypedObject.fromAttributes(targetType, mappedValues, context.schema)
   }

   private fun findScalarInstance(targetType: Type): TypedInstance? {
      // Try searching for it.
      log().debug("Trying to find instance of ${targetType.fullyQualifiedName}")
      val result = queryEngine.find(targetType, context)
      return if (result.isFullyResolved) {
         result[targetType] ?: error("Expected result to contain a ${targetType.fullyQualifiedName} ")
      } else {
         null;
      }
   }
}
