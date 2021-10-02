package io.vyne.query

import arrow.core.extensions.list.functorFilter.filter
import io.vyne.models.AccessorHandler
import io.vyne.models.DataSource
import io.vyne.models.FactDiscoveryStrategy
import io.vyne.models.FailedSearch
import io.vyne.models.MixedSources
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.models.TypedObject
import io.vyne.models.TypedObjectFactory
import io.vyne.models.TypedValue
import io.vyne.models.functions.FunctionRegistry
import io.vyne.query.build.TypedInstancePredicateFactory
import io.vyne.query.collections.CollectionBuilder
import io.vyne.query.collections.CollectionProjectionBuilder
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Field
import io.vyne.schemas.FieldSource
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Type
import io.vyne.utils.log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import lang.taxi.accessors.Accessor
import lang.taxi.types.ObjectType

class ObjectBuilder(
   val queryEngine: QueryEngine,
   val context: QueryContext,
   private val rootTargetType: Type,
   private val functionRegistry: FunctionRegistry = FunctionRegistry.default
) {
   private val buildSpecProvider = TypedInstancePredicateFactory()
   private val originalContext = if (context.isProjecting) context
      .facts
      .firstOrNull { it is TypedObject }
      ?.let {
         val ctx = context.only(it)
         ctx.isProjecting = true
         ctx
      } else null

   private val accessorReaders:List<AccessorHandler<out Accessor>> = listOf(
      CollectionProjectionBuilder(context)
   )
   private val collectionBuilder = CollectionBuilder(queryEngine, context)

   // MP : Can we remove this mutable state somehow?  Let's review later.
   private var manyBuilder: ObjectBuilder? = null

   /**
    * projectionScopeTypes: A list of types that will be used to limit / influence the context of facts
    * when constructing / discovering the targetType.
    * (Currently only supporting when constructing / projecting collections, but more support coming)
    */
   suspend fun build(spec: TypedInstanceValidPredicate = AlwaysGoodSpec, projectionScopeTypes:List<Type> = emptyList()): TypedInstance? {
      val returnValue = build(rootTargetType, spec, projectionScopeTypes)
      return manyBuilder?.build()?.let {
         when (it) {
            is TypedCollection -> TypedCollection.from(listOfNotNull(returnValue).plus(it.value))
            else -> TypedCollection.from(listOfNotNull(returnValue, it))
         }
      } ?: returnValue
   }

   /**
    * projectionScopeTypes: A list of types that will be used to limit / influence the context of facts
    * when constructing / discovering the targetType.
    * (Currently only supporting when constructing / projecting collections, but more support coming)
    */
   private suspend fun build(targetType: Type, spec: TypedInstanceValidPredicate, projectionScopeTypes:List<Type> = emptyList()): TypedInstance? {
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
               log().info(
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

      return if (targetType.isScalar && !targetType.hasExpression) {
         var failedAttempts: List<DataSource>? = null
         findScalarInstance(targetType, spec)
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
      } else if (targetType.isScalar && targetType.hasExpression) {
         // TODO : Do we need the isScalar check there?
         buildExpressionScalar(targetType)
      } else if (targetType.isCollection) {
         buildCollection(targetType, spec, projectionScopeTypes)
      } else {
         buildObjectInstance(targetType, spec, projectionScopeTypes)
      }
   }

   private suspend fun buildExpressionScalar(targetType: Type): TypedInstance? {
      return TypedObjectFactory(
         targetType,
         emptyList<String>(), // What do I pass here?
         context.schema,
         source = MixedSources,
         inPlaceQueryEngine = context
      ).evaluateExpressionType(targetType) /* { // What's this do?
         forSourceValues(sourcedByAttributes, it, targetType)
      } */
   }


   // projectScopeTypes needs to be removed, replaced with a CollectionProjectionExpressionAccessor
   private suspend fun buildCollection(targetType: Type, spec: TypedInstanceValidPredicate, projectionScopeTypes:List<Type>): TypedInstance? {
      val buildResult = collectionBuilder.build(targetType, spec, projectionScopeTypes)
      return buildResult
   }

   // projectScopeTypes needs to be removed, replaced with a CollectionProjectionExpressionAccessor
   private suspend fun build(targetType: QualifiedName, spec: TypedInstanceValidPredicate, projectionScopeTypeNames:List<QualifiedName> = emptyList()): TypedInstance? {
      return build(context.schema.type(targetType), spec, projectionScopeTypeNames.map { context.schema.type(it) })
   }

   private fun convertValue(discoveredValue: TypedInstance, targetType: Type): TypedInstance {
      return if (discoveredValue is TypedValue && ((targetType.hasFormat && targetType.format != discoveredValue.type.format) || targetType.offset != discoveredValue.type.offset)) {
         discoveredValue.copy(targetType)
      } else {
         discoveredValue
      }
   }

   /**
    * projectionScopeTypes: A list of types that will be used to limit / influence the context of facts
    * when constructing / discovering the targetType.
    * (Currently only supporting when constructing / projecting collections, but more support coming)
    */
   private suspend fun buildObjectInstance(targetType: Type, spec: TypedInstanceValidPredicate, projectionScopeTypes:List<Type>): TypedInstance? {
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

      missingAttributes
         .forEach { (attributeName, field) ->
            val buildSpec = buildSpecProvider.provide(field)
            //val attributeContext = originalContext?.only() ?: context
            val targetAttributeType = this.context.schema.type(field.type)
            //val value = ObjectBuilder(this.queryEngine, attributeContext, this.context.schema.type(field.type)).build(buildSpec)
            if (targetAttributeType.hasExpression) {
               // Don't attempt to populate expression types here.
               // The TypedObjectFactory has the expression evaluation logic,
               // so leave the value as un-populated.
            } else {
               val value = build(field.type, buildSpec)
               if (value != null) {
                  populatedValues[attributeName] = convertValue(value, targetAttributeType)
               }
            }
         }

      return TypedObjectFactory(
         targetType,
         populatedValues,
         context.schema,
         source = MixedSources,
         inPlaceQueryEngine = context,
         accessorHandlers = accessorReaders
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
                     functionRegistry = functionRegistry
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
               this.context.schema.type(sourcedBy.attributeType),
               functionRegistry = functionRegistry
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
