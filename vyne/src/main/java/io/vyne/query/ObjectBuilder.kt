package io.vyne.query

import arrow.core.extensions.list.functorFilter.filter
import io.vyne.models.*
import io.vyne.query.build.TypedInstancePredicateFactory
import io.vyne.query.collections.CollectionBuilder
import io.vyne.schemas.*
import io.vyne.utils.log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
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


   private val collectionBuilder = CollectionBuilder(queryEngine,context)
   // MP : Can we remove this mutable state somehow?  Let's review later.
   private var manyBuilder: ObjectBuilder? = null

   suspend fun build(spec: TypedInstanceValidPredicate = AlwaysGoodSpec): TypedInstance? {
      val returnValue = build(rootTargetType, spec)
      return manyBuilder?.build()?.let {
         when (it) {
            is TypedCollection -> TypedCollection.from(listOfNotNull(returnValue).plus(it.value))
            else -> TypedCollection.from(listOfNotNull(returnValue, it))
         }
      } ?: returnValue
   }

   private suspend fun build(targetType: Type, spec: TypedInstanceValidPredicate): TypedInstance? {
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
               log().error(
                  "Found ${instance.size} instances of ${targetType.fullyQualifiedName}. Values are ${
                     instance.map {
                        Pair(
                           it.typeName,
                           it.value
                        )
                     }.joinToString()
                  }"
               )
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
            .catch { exception ->
               when (exception) {
                  is SearchFailedException -> log().debug(exception.message)
                  else -> log().error(
                     "An exception occurred whilst searching for type ${targetType.fullyQualifiedName}",
                     exception
                  )
               }
            }
            .firstOrNull()
      } else if (targetType.isCollection) {
         buildCollection(targetType,spec)
      } else {
         buildObjectInstance(targetType, spec)
      }
   }

   private suspend fun buildCollection(targetType: Type, spec: TypedInstanceValidPredicate): TypedInstance? {
      val buildResult = collectionBuilder.build(targetType,spec)
      return buildResult
   }

   private suspend fun build(targetType: QualifiedName, spec: TypedInstanceValidPredicate): TypedInstance? {
      return build(context.schema.type(targetType), spec)
   }

   private fun convertValue(discoveredValue: TypedInstance, targetType: Type): TypedInstance {
      return if (discoveredValue is TypedValue && ((targetType.hasFormat && targetType.format != discoveredValue.type.format) || targetType.offset != discoveredValue.type.offset)) {
         discoveredValue.copy(targetType)
      } else {
         discoveredValue
      }
   }

   private suspend fun buildObjectInstance(targetType: Type, spec: TypedInstanceValidPredicate): TypedInstance? {
      val populatedValues = mutableMapOf<String, TypedInstance>()
      val missingAttributes = mutableMapOf<AttributeName, Field>()
      // contains the anonymous projection attributes for:
      // traderEmail : EmailAddress(from this.traderUtCode)
      val sourcedByAttributes = mutableMapOf<AttributeName, Field>()
      // =============================================================
      // TODO think how to fix it properly
      // Quick and dirty fix for projection of ObjectType to another ObjectType
      // If source and target types are ObjectTypes, just copy properties in one iteration
      // With this fix projection time of 1000 items was reduced from 37seconds to 650ms!
      // Enum filtering will be removed once the updated enum processing logic branch is merged.
      if (targetType.taxiType is ObjectType && context.facts.filter { !it.type.isEnum }.size == 1) {
         val sourceObjectType = context.facts.filter { !it.type.isEnum }.iterator().next()
         if (sourceObjectType is TypedObject) {
            targetType
               .attributes
               .forEach { (attributeName, field) ->
                  if (field.sourcedBy == null) {
                     val fieldInstanceValidPredicate = buildSpecProvider.provide(field)
                     val targetAttributeType = context.schema.type(field.type)
                     val returnTypedNull = true
                     when (val value =
                        sourceObjectType.getAttributeIdentifiedByType(targetAttributeType, returnTypedNull)) {
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
                  } else {
                     sourcedByAttributes[attributeName] = field
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
            populatedValues[attributeName] = value
//            if (value.type.isCollection) {
//               val typedCollection = value as TypedCollection?
//               typedCollection?.let {
//                  populatedValues[attributeName] = it.first()
//                  this.originalContext?.let {
//                     manyBuilder = ObjectBuilder(queryEngine, originalContext, targetType)
//                  }
//               }
//            } else {
//               populatedValues[attributeName] = value
//            }
         }
      }

      return TypedObjectFactory(
         targetType,
         populatedValues,
         context.schema,
         source = MixedSources
      ).buildAsync {
         forSourceValues(sourcedByAttributes, it, targetType)
      }
   }

   private suspend fun forSourceValues(
      sourcedByAttributes: Map<AttributeName, Field>,
      attributeMap: Map<AttributeName, TypedInstance>,
      targetType: Type
   ):
      Map<AttributeName, TypedInstance> {
      val sourcedValues = sourcedByAttributes.mapNotNull { (attributeName, field) ->
         val sourcedBy = field.sourcedBy!!
         if (sourcedBy.sourceType != targetType.qualifiedName) {
            val sourceFact =
               this.context.facts.firstOrNull { fact -> fact.typeName == sourcedBy.sourceType.fullyQualifiedName && fact is TypedObject }
            sourceFact?.let { typedInstance -> fromDiscoveryType(typedInstance, sourcedBy, attributeName) }
         } else {
            attributeMap[sourcedBy.attributeName]?.let { source ->
               source.value?.let { _ ->
                  ObjectBuilder(
                     this.queryEngine,
                     this.context.only(source),
                     this.context.schema.type(sourcedBy.attributeType)
                  )
                     .build()?.let {
                        return@mapNotNull attributeName to it
                     }
               }
            }
         }
      }.toMap()

      return if (sourcedValues.isNotEmpty()) {
         attributeMap.plus(sourcedValues)
      } else {
         attributeMap
      }
   }

   private suspend fun fromDiscoveryType(
      typedInstance: TypedInstance,
      sourcedBy: FieldSource,
      attributeName: AttributeName
   ): Pair<AttributeName, TypedInstance>? {
      val typedObject = typedInstance as TypedObject
      typedObject[sourcedBy.attributeName]?.let { source ->
         source.value?.let { _ ->
            ObjectBuilder(
               this.queryEngine,
               this.context.only(source),
               this.context.schema.type(sourcedBy.attributeType)
            )
               .build()?.let {
                  return attributeName to it
               }
         }
      }
      return null
   }

   private suspend fun findScalarInstance(targetType: Type, spec: TypedInstanceValidPredicate): Flow<TypedInstance> {
      // Try searching for it.
      //log().debug("Trying to find instance of ${targetType.fullyQualifiedName}")
      val result = try {
         queryEngine.find(targetType, context, spec)
      } catch (e: Exception) {
         log().error("Failed to find type ${targetType.fullyQualifiedName}", e)
         null
      }
      //return if (result?.isFullyResolved) {
      return result?.results ?: error("Expected result to contain a ${targetType.fullyQualifiedName} ")
      //} else {
      //   null
      //}
   }


}
