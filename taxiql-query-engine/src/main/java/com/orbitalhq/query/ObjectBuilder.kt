package com.orbitalhq.query

import com.orbitalhq.models.*
import com.orbitalhq.models.facts.FactBag
import com.orbitalhq.models.facts.FactDiscoveryStrategy
import com.orbitalhq.models.facts.FieldAndFactBag
import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.models.functions.FunctionRegistry
import com.orbitalhq.query.ExcludeQueryStrategyKlassPredicate.Companion.ExcludeObjectBuilder
import com.orbitalhq.query.collections.CollectionBuilder
import com.orbitalhq.query.collections.CollectionProjectionBuilder
import com.orbitalhq.schemas.*
import com.orbitalhq.schemas.taxi.toVyneQualifiedName
import com.orbitalhq.utils.log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import lang.taxi.accessors.Accessor
import lang.taxi.accessors.CollectionProjectionExpressionAccessor
import lang.taxi.accessors.ConditionalAccessor
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.ObjectType
import mu.KotlinLogging
import java.util.*

class ObjectBuilder(
   val queryEngine: QueryEngine,
   val context: QueryContext,
   private val rootTargetType: Type,
   private val functionRegistry: FunctionRegistry = FunctionRegistry.default,
   private val formatSpecs: List<ModelFormatSpec>,
) {
   private val logger = KotlinLogging.logger {}
   private val id = UUID.randomUUID().toString()
   private val buildSpecProvider = TypedInstancePredicateFactory()


   private val accessorReaders: List<AccessorHandler<out Accessor>> = listOf(
      CollectionProjectionBuilder(context)
   )
   private val collectionBuilder = CollectionBuilder(queryEngine, context)

   init {
      log().debug("[${context.queryId}] ObjectBuilder $id created to build ${rootTargetType.name.longDisplayName}")
   }

   suspend fun build(
      spec: TypedInstanceValidPredicate = AlwaysGoodSpec
   ): TypedInstance? {
      return build(rootTargetType, spec)
   }

   private suspend fun build(
      targetType: Type,
      spec: TypedInstanceValidPredicate,
      // Passing facts here allows for reference to data from parent objects when constructing child objects
      facts: FactBag = FactBag.empty()
   ): TypedInstance? {
      val nullableFact = context.getFactOrNull(targetType, FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY, spec)
      if (nullableFact != null) {
         val instance = nullableFact as TypedCollection
         when (instance.size) {
            0 -> error("Found 0 instances of ${targetType.fullyQualifiedName}, but hasFactOfType returned true")
            1 -> {
               // FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY always returns a collection.
               // If we didn't request a collection, we actually want the first element.
               // Otherwise, we want the collection itself.
               val discoveredValue = if (targetType.isCollection) {
                  instance
               } else {
                  instance.first()
               }
               // Handle formatting
               // Choosing to copy the type, since as we got this far, we know that
               // the types are compatible.  However, this may prove to cause problems.
               return convertValue(discoveredValue, targetType, format = null)
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
               if (!targetType.isPrimitive) {
                  // Don't bother logging if the user searched for a primitive type, as it's kinda pointless.
                  logger.debug { "Found ${instance.size} instances of ${targetType.longDisplayName}." }
                  logger.trace {
                     "Found ${instance.size} instances of ${targetType.longDisplayName}. Values are ${
                        instance.map {
                           Pair(
                              it.typeName,
                              it.value
                           )
                        }.joinToString()
                     }"
                  }
               }

               // HACK : How do we handle this?
               return if (nonNullMatches.isNotEmpty()) {
                  // Since notNullMatches !empty && size > 1, this should be a collection, so return it as such.
                  // Note: 13-Jun-2022 Previously, this used to just return *just the first* item from the collection.
                  // This has now been fixed to behave correctly, but may cause regression behaviour.
                  // see VyneCollectionDiscoveryTest.kt - `one to many projection works`()
                  val dataSource = nonNullMatches.first().source
                  TypedCollection.arrayOf(targetType, nonNullMatches, dataSource)
                  //nonNullMatches.first()
               } else {
                  // Case for all matches are TypedNull.
                  null
               }
            }
         }
      }

      return if (targetType.isScalar && !targetType.hasExpression) {
         // if (allowRecursion)  searchForType(targetType, spec) else null
         searchForType(targetType, spec)
      } else if (targetType.isScalar && targetType.hasExpression) {
         // TODO : Do we need the isScalar check there?
         buildExpressionScalar(targetType)
      } else if (targetType.isCollection) {
         buildCollection(targetType, spec)
      } else {
         findOrBuildObjectInstance(targetType, spec, facts)
      }
   }

   private suspend fun searchForType(
      targetType: Type,
      spec: TypedInstanceValidPredicate,
      facts: FactBag = FactBag.empty()
   ): TypedInstance? {
      var failedAttempts: List<DataSource>? = null
      return findInstance(targetType, spec, facts)
         .catch { exception ->
            when (exception) {
               is SearchFailedException -> {
                  log().debug(exception.message)
                  failedAttempts = exception.failedAttempts
               }

               else -> log().error(
                  "An exception occurred whilst searching for type ${targetType.fullyQualifiedName}",
                  exception
               )
            }
         }
         .firstOrNull().let { instance: TypedInstance? ->
            if (instance == null && failedAttempts != null) {
               context.vyneQueryStatistics.graphSearchFailedCount.addAndGet(failedAttempts!!.size)
               TypedNull.create(
                  targetType,
                  FailedSearch("The search failed after ${failedAttempts!!.size} attempts", failedAttempts!!)
               )
            } else {
               instance
            }
         }
   }

   private fun buildExpressionScalar(targetType: Type): TypedInstance {
      return TypedObjectFactory(
         targetType,
         emptyList<String>(), // What do I pass here?
         context.schema,
         source = MixedSources,
         inPlaceQueryEngine = context,
         formatSpecs = formatSpecs,
         functionResultCache = context.functionResultCache
      ).evaluateExpressionType(targetType, null) /* { // What's this do?
         forSourceValues(sourcedByAttributes, it, targetType)
      } */
   }


   // projectScopeTypes needs to be removed, replaced with a CollectionProjectionExpressionAccessor
   private suspend fun buildCollection(
      targetType: Type,
      spec: TypedInstanceValidPredicate
   ): TypedInstance? {
      return if (targetType.collectionType?.expression is CollectionProjectionExpressionAccessor) {
         buildCollectionWithProjectionExpression(targetType)
      } else {
         collectionBuilder.build(targetType, spec)
      }

   }

   /**
    * Builds collections where we're iterating another collection
    * eg:
    *  find { OrderTransaction[] } as {
    *   items: Thing[] by [ThingToIterate[]   with { CustomerName }]
    * }[]
    */
   private fun buildCollectionWithProjectionExpression(targetType: Type): TypedInstance {
      val collectionProjectionBuilder = accessorReaders.filterIsInstance<CollectionProjectionBuilder>().firstOrNull()
         ?: error("No CollectionProjectionBuilder was present in the acessor readers")
      return collectionProjectionBuilder.process(
         targetType.collectionType?.expression!! as CollectionProjectionExpressionAccessor,
         TypedObjectFactory(
            targetType.collectionType!!,
            context.facts,
            context.schema,
            source = MixedSources,
            inPlaceQueryEngine = context,
            functionResultCache = context.functionResultCache
         ),
         context.schema,
         targetType,
         MixedSources
      )
   }

   private suspend fun build(
      targetType: QualifiedName,
      spec: TypedInstanceValidPredicate,
      // Passing facts here allows for reference to data from parent objects when constructing child objects
      facts: FactBag = FactBag.empty()
   ): TypedInstance? {
      return build(context.schema.type(targetType), spec, facts)
   }

   private fun convertValue(
      discoveredValue: TypedInstance,
      targetType: Type,
      format: FormatsAndZoneOffset?
   ): TypedInstance {
      return if (discoveredValue is TypedValue && format != null && format != discoveredValue.format) {
         discoveredValue.copy(format = format)
      } else {
         discoveredValue
      }
   }

   private suspend fun findOrBuildObjectInstance(
      targetType: Type,
      spec: TypedInstanceValidPredicate,
      // Passing facts here allows for reference to data from parent objects when constructing child objects
      facts: FactBag = FactBag.empty()
   ): TypedInstance {
      val result = searchForType(targetType, spec, facts)
      val searchFailed = result == null || result is TypedNull && result.source is FailedSearch
      return if (searchFailed) {
         if (!targetType.isClosed) {
            logger.debug { "Search for object ${targetType.qualifiedName.shortDisplayName} failed, so initiating building one" }
            val result = buildObjectInstance(targetType, spec, facts)
            logger.debug { "Successfully built instance of ${targetType.qualifiedName.shortDisplayName}" }
            result
         } else {
            TypedNull.create(
               targetType,
               FailedSearch("Unable to find a data source for ${targetType.qualifiedName.shortDisplayName} and building is not permitted because the type is closed")
            )
         }
      } else {
         result!!
      }
   }

   private suspend fun buildObjectInstance(
      targetType: Type,
      spec: TypedInstanceValidPredicate,
      // Passing facts here allows for reference to data from parent objects when constructing child objects
      facts: FactBag = FactBag.empty(),
   ): TypedInstance {
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
                     val targetAttributeType = field.resolveType(context.schema)
                     val returnTypedNull = true
                     when (val value =
                        sourceObjectType.getAttributeIdentifiedByType(targetAttributeType, returnTypedNull)) {
                        is TypedNull -> missingAttributes[attributeName] = field
                        else -> {
                           val attributeSatisfiesPredicate = fieldInstanceValidPredicate.isValid(value)
                           if (attributeSatisfiesPredicate) {
                              populatedValues[attributeName] = convertValue(value, targetAttributeType, field.format)
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

      missingAttributes
         .forEach { (attributeName, field) ->
            val buildSpec = buildSpecProvider.provide(field)
            //val attributeContext = originalContext?.only() ?: context
            val targetAttributeType = field.resolveType(context.schema)
            //val value = ObjectBuilder(this.queryEngine, attributeContext, this.context.schema.type(field.type)).build(buildSpec)
            if (targetAttributeType.hasExpression) {
               // Don't attempt to populate expression types here.
               // The TypedObjectFactory has the expression evaluation logic,
               // so leave the value as un-populated.
            } else if (field.accessor is ConditionalAccessor) {
               // Don't attempt to populate fields with ConditionalAccessor here.
               // The TypedObjectFactory has the expression evaluation logic,
               // so leave the value as un-populated.
            } else {
               // We need to pass parent facts around, so that when constructing nested objects, children fields have reference to parent facts.
               val theseFacts =
                  FieldAndFactBag(populatedValues, emptyList(), context.scopedFacts, context.schema).merge(facts)

               // When building a field, populate the source (unprojected) type.
               // When we go to construct the final object (In TypedObjectFactory), this source
               // value will be projected to it's actual type.
               val fieldBuildType =
                  field.fieldProjection?.sourceType?.toVyneQualifiedName()?.let { context.schema.type(it) }
                     ?: field.resolveType(context.schema)
               val value = build(fieldBuildType, buildSpec, theseFacts)


               if (value != null) {
                  populatedValues[attributeName] = convertValue(value, targetAttributeType, field.format)
//                  log().debug("Object builder ${this.id} populated attribute $attributeName : ${targetAttributeType.name.longDisplayName}.  Now contains keys: ${populatedValues.keys}")
               }
            }
         }


      // MP 2-Nov-21:  We used to pass populatedValues here, which is a map containing field names.
      // However, we want all the facts discovered in the context to be includable in the search when building
      // objects.
      // So, trying with a special type of FactBag.
      // We needed a new FactBag type here, as we need to retain the field name information.
      val searchableFacts =
         FieldAndFactBag(populatedValues, context.facts.toList(), context.scopedFacts, context.schema)
      val searchableWithParentFacts = searchableFacts.merge(facts)


      return TypedObjectFactory(
         targetType,
         searchableWithParentFacts,
         context.schema,
         source = MixedSources,
         inPlaceQueryEngine = context,
         accessorHandlers = accessorReaders,
         formatSpecs = formatSpecs,
         functionResultCache = context.functionResultCache
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
                     this.context.schema.type(sourcedBy.attributeType),
                     functionRegistry = functionRegistry,
                     formatSpecs = formatSpecs
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
      typedObject[sourcedBy.attributeName].let { source ->
         source.value?.let { _ ->
            ObjectBuilder(
               this.queryEngine,
               this.context.only(source),
               sourcedBy.attributeAnonymousType ?: this.context.schema.type(sourcedBy.attributeType),
               functionRegistry = functionRegistry,
               formatSpecs = formatSpecs,
            )
               .build()?.let {
                  return attributeName to it
               }
         }
      }
      return null
   }

   private suspend fun findInstance(
      targetType: Type,
      spec: TypedInstanceValidPredicate,
      facts: FactBag,
   ): Flow<TypedInstance> {
      // Try searching for it.
      //log().debug("Trying to find instance of ${targetType.fullyQualifiedName}")
      val result = try {
//         val queryContext = context.copy()
//         queryContext.addFacts(facts.rootAndScopedFacts())
         queryEngine.find(targetType, context, spec, ExcludeObjectBuilder)
      } catch (e: QueryCancelledException) {
         throw e
      } catch (e: Exception) {
         log().error("Failed to find type ${targetType.fullyQualifiedName}", e)
         null
      }
      //return if (result?.isFullyResolved) {
      return result?.results ?: error("Expected result to contain a ${targetType.fullyQualifiedName} ")
   }


}