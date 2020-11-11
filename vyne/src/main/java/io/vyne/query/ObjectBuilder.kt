package io.vyne.query

import arrow.core.extensions.list.functorFilter.filter
import io.vyne.models.*
import io.vyne.query.build.TypedInstancePredicateFactory
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Field
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Type
import io.vyne.utils.log
import lang.taxi.types.ObjectType

class ObjectBuilder(val queryEngine: QueryEngine, val context: QueryContext, private val rootTargetType: Type) {
   private val buildSpecProvider = TypedInstancePredicateFactory()
   private val originalContext = if (context.isProjecting) context
      .facts
      .firstOrNull { it is TypedObject }
      ?.let {
         val ctx = context.only(it)
         ctx.isProjecting = true
         ctx
      } else null


   private var manyBuilder: ObjectBuilder? = null

   fun build(spec: TypedInstanceValidPredicate = AlwaysGoodSpec): TypedInstance? {
      val returnValue = build(rootTargetType,spec)
      return manyBuilder?.build()?.let {
         when (it) {
            is TypedCollection -> TypedCollection.from(listOfNotNull(returnValue).plus(it.value))
            else -> TypedCollection.from(listOfNotNull(returnValue, it))
         }
      } ?: returnValue
   }

   private fun build(targetType: Type, spec: TypedInstanceValidPredicate): TypedInstance? {
      val nullableFact = context.getFactOrNull(targetType, FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY, spec)
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
            else -> {
               // last attempt
               val exactMatches = instance.filter { it.type == targetType }
               if (exactMatches.size == 1) {
                  return exactMatches.first()
               }

               val nonNullMatches = instance.filter { it.value != null }
               if (nonNullMatches.size == 1) {
                  return nonNullMatches.first()
               }
               log().error("Found ${instance.size} instances of ${targetType.fullyQualifiedName}. Values are ${instance.map { Pair(it.typeName, it.value)}.joinToString()}")
               // HACK : How do we handle this?
               return if (nonNullMatches.isNotEmpty()) {
                  nonNullMatches.first()
               } else {
                  // Case for all matches are TypedNull.
                  null
               }
            }
         }
      }

      return if (targetType.isScalar) {
         findScalarInstance(targetType, spec)
      } else {
         buildObjectInstance(targetType, spec)
      }
   }

   private fun build(targetType: QualifiedName, spec:TypedInstanceValidPredicate): TypedInstance? {
      return build(context.schema.type(targetType), spec)
   }

   private fun convertValue(discoveredValue: TypedInstance, targetType: Type): TypedInstance {
      return if (discoveredValue is TypedValue && ( (targetType.hasFormat && targetType.format != discoveredValue.type.format) || targetType.offset != discoveredValue.type.offset) ) {
         discoveredValue.copy(targetType)
      } else {
         discoveredValue
      }
   }

   private fun buildObjectInstance(targetType: Type, spec: TypedInstanceValidPredicate): TypedInstance? {
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
               val fieldInstanceValidPredicate = buildSpecProvider.provide(field)
               val targetAttributeType = context.schema.type(field.type)
               val returnTypedNull = true
               when (val value = sourceObjectType.getAttributeIdentifiedByType(targetAttributeType, returnTypedNull)) {
                  is TypedNull -> missingAttributes[attributeName] = field
                  else -> {
                     val attributeSatisfiesPredicate = fieldInstanceValidPredicate.isValid(value)
                     if (attributeSatisfiesPredicate) {
                        populatedValues[attributeName] = convertValue(value, targetAttributeType)
                     } else {
                        missingAttributes[attributeName] = field
                     }

                  }
               }
            }
         } else {
            missingAttributes.putAll(targetType.attributes)
         }
      } else {
         targetType.attributes.forEach { (attributeName, field) ->
            missingAttributes[attributeName] = field
         }
      }

      missingAttributes.forEach { (attributeName, field) ->
         val buildSpec = buildSpecProvider.provide(field)
         val value = build(field.type, buildSpec)
         if (value != null) {
            if (value.type.isCollection) {
               val typedCollection = value as TypedCollection?
               typedCollection?.let {
                  populatedValues[attributeName] = it.first()
                  this.originalContext?.let {
                     manyBuilder = ObjectBuilder(queryEngine, originalContext, targetType)
                  }
               }
            } else {
               populatedValues[attributeName] = value
            }
         }
      }

      return TypedObjectFactory(targetType, populatedValues, context.schema, source = MixedSources).build()

//      return TypedObject(targetType, populatedValues, MixedSources)
   }

   private fun findScalarInstance(targetType: Type, spec:TypedInstanceValidPredicate): TypedInstance? {
      // Try searching for it.
      //log().debug("Trying to find instance of ${targetType.fullyQualifiedName}")
      val result = try {
         queryEngine.find(targetType, context, spec)
      } catch (e:Exception) {
         log().error("Failed to find type ${targetType.fullyQualifiedName}", e)
         return null
      }
      return if (result.isFullyResolved) {
         result[targetType] ?: error("Expected result to contain a ${targetType.fullyQualifiedName} ")
      } else {
         null
      }
   }
}
