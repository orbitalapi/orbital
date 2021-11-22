package io.vyne.query.collections

import io.vyne.models.FactDiscoveryStrategy
import io.vyne.models.FactSearch
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.query.QueryContext
import io.vyne.query.QueryEngine
import io.vyne.query.SearchFailedException
import io.vyne.query.TypedInstanceValidPredicate
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Field
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Used by the ObjectBuilder for creating a collection of things
 */
class CollectionBuilder(val queryEngine: QueryEngine, val queryContext: QueryContext) {
   companion object {
      val ID_ANNOTATION = "Id".fqn()
   }

   suspend fun build(
      targetType: Type,
      spec: TypedInstanceValidPredicate
   ): TypedInstance? {
      val targetMemberType = targetType.collectionType
         ?: error("Type ${targetType.fullyQualifiedName} returned true for isCollection, but did not expose a collectionType")

      val fromSearchByCollectionType = searchUsingCollectionType(targetType, queryContext, spec)
      if (fromSearchByCollectionType != null) {
         return fromSearchByCollectionType
      }


      // TODO :  This is possibly a good place to consider joinTo() clauses.

      // Does the type we're looking for have an @Id?
      val idFields = (targetMemberType.attributes.filter { (_, field) ->
         isIdField(field)
      })

      if (idFields.isNotEmpty()) {
         return searchUsingIdField(targetMemberType, queryContext, spec, idFields)
      }

      val buildFromIdValues = attemptBuildUsingCollectionOfIds(targetMemberType)
      if (buildFromIdValues != null) {
         return buildFromIdValues
      }

      val buildFromSimilarBaseType = attemptBuildingCollectionOfSimilarBaseTypeInstances(targetMemberType)
      if (buildFromSimilarBaseType != null) {
         return buildFromSimilarBaseType
      }


      // TODO : Other strategies..
      return null
   }

   private suspend fun searchUsingCollectionType(
      targetType: Type,
      queryContext: QueryContext,
      spec: TypedInstanceValidPredicate
   ): TypedInstance? {
      val queryResult = queryEngine.find(targetType, queryContext, spec)
      val resultList = try {
         queryResult.results
            .toList()
      } catch (e:SearchFailedException) {
         return null
      }

      // MP: Experiment - knowing what to return here is difficult
      // If the search has succeeced with an empty list, was that because it
      // found an empty list?
      return if (resultList.isEmpty()) {
         TypedCollection.empty(targetType)
      } else if (resultList.size == 1 && resultList[0] is TypedCollection) {
         resultList[0] as TypedCollection
      } else {
         TypedCollection.from(resultList)
      }
   }

   private suspend fun attemptBuildingCollectionOfSimilarBaseTypeInstances(targetMemberType: Type): TypedInstance? {
      val collectionOfFactsWithCommonBaseType = targetMemberType.inherits
         .asSequence()
         .filter { !it.isPrimitive }
         .mapNotNull { baseType ->
            val filterPredicate: (TypedInstance) -> Boolean = { instance ->
               instance is TypedCollection && instance.type.collectionType!!.inheritsFrom(baseType)
            }
            val collectionOfFactsWithCommonBaseType = queryContext.getFactOrNull(
               FactSearch(
                  "Collection of @Id annotated values",
                  FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY,
                  filterPredicate
               )
            )
            collectionOfFactsWithCommonBaseType
         }
         .firstOrNull() ?: return null

      require(collectionOfFactsWithCommonBaseType is TypedCollection)
      val instancesMappedToTargetType = collectionOfFactsWithCommonBaseType
         .asFlow()
         .flatMapConcat { sourceInstance ->
            queryContext.only(sourceInstance).build(targetMemberType.qualifiedName.parameterizedName)
               .results
         }.toList()
      val collectionOfTargetType = TypedCollection.from(instancesMappedToTargetType)
      return collectionOfTargetType
   }

   /**
    * In this strategy, we look in the context for a collection of @Id annotated values
    * which are able to build the requested type.
    *
    * Note - if we find mulitple collections of @Id values, we use the first one to generate any results.
    */
   private suspend fun attemptBuildUsingCollectionOfIds(targetType: Type): TypedInstance? {
      // Do we have a collection of ids, whose type can be used to find the thing we're after?
      // This covers the use case where we have an Id, and want a field from an entity that's identified by
      // what we're looking for.
      // TODO : This search can be cached.
      val modelsWithIds = findModelsWithIdFields()
      // This is a collection of collections of Id's.  ie: List<List<Id>>, as there could be multiple Id collections
      // in our set of facts, and we don't yet know which one would yeild the data we're after
      val collectionOfIdCollections = findCollectionsOfIdValues(modelsWithIds)

      if (collectionOfIdCollections != null && collectionOfIdCollections is TypedCollection) {
         val idCollections = collectionOfIdCollections.value as List<TypedCollection>
         val builtInstances: TypedCollection? = idCollections
            .asFlow()
            .map { idCollection ->
               idCollection.map { idValue ->
                  withContext(Dispatchers.IO) {
                     val built =
                        queryContext.only(idValue).build(targetType.qualifiedName).results.toList().firstOrNull()
                     built
                  }
               }
            }
            .map { typedInstances -> typedInstances.filterNotNull() }
            .firstOrNull { typedInstances -> typedInstances.isNotEmpty() }
            ?.let { TypedCollection.from(it) }
         return builtInstances
      } else {
         return null
      }
   }

   private fun findCollectionsOfIdValues(modelsWithIds: Map<Field, Type>): TypedInstance? {
      val idTypeNames = modelsWithIds.keys.map { it.type }.toSet()
      val filterPredicate: (TypedInstance) -> Boolean = { instance ->
         instance is TypedCollection && idTypeNames.contains(instance.type.collectionType!!.qualifiedName)
      }
      val collectionOfIds = queryContext.getFactOrNull(
         FactSearch(
            "Collection of @Id annotated values",
            FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY,
            filterPredicate
         )
      )
      return collectionOfIds
   }

   private fun findModelsWithIdFields(): Map<Field, Type> {
      val modelsWithIds = queryContext.schema.types.mapNotNull { type ->
         val idFields = type.attributes.filter { (_, field) -> isIdField(field) }
         when (idFields.size) {
            0 -> null
            1 -> idFields.values.first() to type
            else -> {
               logger.warn { "Type ${type.fullyQualifiedName} has multiple id fields, which is not currently supported when building " }
               null
            }
         }
      }.toMap()
      return modelsWithIds
   }

   private fun isIdField(field: Field): Boolean {
      val fieldType = queryContext.schema.type(field.type)
      return fieldType.hasMetadata(ID_ANNOTATION) || field.hasMetadata(
         ID_ANNOTATION
      )
   }

   private suspend fun searchUsingIdField(
      targetMemberType: Type,
      context: QueryContext,
      spec: TypedInstanceValidPredicate,
      idFields: Map<AttributeName, Field>
   ): TypedInstance? {
      if (idFields.size > 1) {
         logger.warn { "Attempting to search for a collection of ${targetMemberType.fullyQualifiedName} and found mulitple id fields (${idFields.map { it.key }}).  This is not yet supported, will abort" }
         return null
      }
      val idFieldType = context.schema.type(idFields.values.first().type)
      val discoveredIdValues = context.getFactOrNull(idFieldType, FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY)

      if (discoveredIdValues is TypedCollection) {
         val discoveredInstanceValues = discoveredIdValues.value.flatMap { idValue ->
            context.only(idValue).build(targetMemberType.name).results.toList()
         }
         return TypedCollection.from(discoveredInstanceValues)
      }
      // TODO
      return null
   }
}
