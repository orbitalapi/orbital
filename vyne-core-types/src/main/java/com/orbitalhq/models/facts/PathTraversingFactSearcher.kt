package com.orbitalhq.models.facts

import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedEnumValue
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.TypedObject
import com.orbitalhq.query.TypedInstanceValidPredicate
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.types.ArrayType
import lang.taxi.types.EnumType
import lang.taxi.types.EnumValue
import lang.taxi.types.ObjectType
import lang.taxi.types.PrimitiveType
import lang.taxi.types.UnionType
import mu.KotlinLogging
import org.eclipse.collections.api.map.ConcurrentMutableMap
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap

private typealias SchemaHash = Int
private typealias TypeTraversalKey = String

/**
 * A cache of traversal strategies, which
 * determine the fields to navigate moving from sourceType to targetType
 */
interface TypeTraversalPathCache {
   fun getTypeTraversal(
      schema: Schema,
      sourceType: Type,
      targetType: Type,
      strategy: FactDiscoveryStrategy
   ): NavigatableTypeAttributePath?

   fun clear()
   fun clearSchema(schema: Schema)
}

object GlobalSchemaFactSearchCache : TypeTraversalPathCache {
   private val cache: TypeTraversalPathCache = DefaultSchemaFactSearchCache()
   override fun getTypeTraversal(
      schema: Schema,
      sourceType: Type,
      targetType: Type,
      strategy: FactDiscoveryStrategy
   ): NavigatableTypeAttributePath? = cache.getTypeTraversal(schema, sourceType, targetType, strategy)

   override fun clear() = cache.clear()

   override fun clearSchema(schema: Schema) = cache.clearSchema(schema)
}

class DefaultSchemaFactSearchCache : TypeTraversalPathCache {
   private val cache: ConcurrentMutableMap<SchemaHash, ConcurrentMutableMap<TypeTraversalKey, NavigatableTypeAttributePath>> =
      ConcurrentHashMap()

   companion object {
      private val logger = KotlinLogging.logger {}
   }


   override fun clear() {
      cache.clear()
   }

   override fun clearSchema(schema: Schema) {
      cache.remove(schema.hash)
   }

   private fun navigationKey(sourceType: Type, targetType: Type) =
      "${sourceType.name.parameterizedName}->${targetType.name.parameterizedName}"

   override fun getTypeTraversal(
      schema: Schema,
      sourceType: Type,
      targetType: Type,
      strategy: FactDiscoveryStrategy
   ): NavigatableTypeAttributePath? {
      val traversalStrategies = cache.getOrPut(schema.hash) {
         ConcurrentHashMap()
      }
      val navigationKey = navigationKey(sourceType, targetType)
      return traversalStrategies.getOrPut(navigationKey) {
         buildNavigatableAttributePath(navigationKey, sourceType, targetType, strategy, schema)
      }
   }

   /**
    * Responsible for actually constructing the NavigatableTypeAttributePath
    * containing all the possible routes from SourceType to TargetType.
    */
   private fun buildNavigatableAttributePath(
      navigationKey: String,
      sourceType: Type,
      targetType: Type,
      strategy: FactDiscoveryStrategy,
      schema: Schema
   ): NavigatableTypeAttributePath? {
      val sourceTaxiType = sourceType.taxiType
      val targetTaxiType = targetType.taxiType

      if (sourceTaxiType.isAssignableTo(targetTaxiType)) {
         return NavigatableTypeAttributePath(navigationKey, listOf(ReadSelf), targetType)
      }
      if (targetType.taxiType == PrimitiveType.ANY) {
         return null // don't search for ANY
      }


      val pathsToInstances = if (targetTaxiType is EnumType) {
         // For enum types we also have to search for synonyms

         val otherTypesToSearch = targetTaxiType.values.flatMap {
            it.synonyms.map { enumSynonymName ->
               EnumValue.splitEnumValueName(enumSynonymName).first
            }
         }.distinct()
            .map { enumTypeName -> schema.type(enumTypeName.parameterizedName).taxiType }

         (listOf(targetTaxiType) + otherTypesToSearch)
            .flatMap { targetOrSynonymTargetType ->
               getPath(sourceType, schema.type(targetOrSynonymTargetType), strategy, schema) ?: emptyList()
            }
      } else {
         getPath(sourceType, targetType, strategy, schema)
      }

      // eg: Search for FirstName[] should gather all the FirstName instances present
      // in the fact set
      val pathsToInstancesOfCollectionMemberType =
         if (targetType.isCollection && strategy == FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY) {
            getPath(sourceType, unwrapCollectionType(targetType), strategy, schema)
         } else null
      if (pathsToInstances == null && pathsToInstancesOfCollectionMemberType == null) {
         logger.debug { "No navigable path from type  ${sourceType.qualifiedName.shortDisplayName}  to ${targetType.qualifiedName.shortDisplayName}" }
         return null
      }

      val allPaths = (pathsToInstances ?: emptyList()) + (pathsToInstancesOfCollectionMemberType ?: emptyList())

      val navigators = allPaths
         .distinct()
         .map { attributePath ->
            val parts = attributePath.split(".")
            val attributePathsAndTypes: MutableList<Pair<String, Type>> =
               parts.fold(mutableListOf<Pair<String, Type>>("" to sourceType)) { acc, attributeName ->
                  val (_, lastType) = acc.last()
                  if (ReadEnumSynonym.isInstructionToken(attributeName)) {
                     acc.add(attributeName to lastType)
                     return@fold acc
                  }
                  if (ReadSelf.isInstructionToken(attributeName)) {
                     acc.add(attributeName to lastType)
                     return@fold acc
                  }

                  val typeToNavigate = unwrapCollectionType(lastType)
                  if (!typeToNavigate.hasAttribute(attributeName)) {
                     error("The provided path is invalid. The path that was build is $attributePath but type ${typeToNavigate.name.shortDisplayName} has no attribute named $attributeName")
                  }
                  val field = typeToNavigate.attribute(attributeName)
                  val fieldType = schema.type(field.type)
                  acc.add(attributeName to fieldType)
                  acc
               }
            val pathNavigators = attributePathsAndTypes.mapIndexed { index, (attributeName, pathType) ->
               val isLast = index == attributePathsAndTypes.size - 1
               when {
                  ReadEnumSynonym.isInstructionToken(attributeName) -> {
                     ReadEnumSynonym.fromInstruction(attributeName, schema)
                  }

                  ReadSelf.isInstructionToken(attributeName) -> ReadSelf

                  attributeName.isEmpty() -> {
                     if (sourceType.isCollection) {
                        IterateSelf
                     } else {
                        ReadSelf
                     }
                  }

                  pathType.isCollection -> {
                     when {
                        isLast && targetType.isCollection -> ReadField(attributeName)
                        else -> IterateArray(attributeName)
                     }
                  }

                  else -> ReadField(attributeName)
               }
            }
            CompositePathAttributeNavigator(attributePath, pathNavigators)
         }

      return NavigatableTypeAttributePath(navigationKey, navigators, targetType)
   }

   private fun getPath(
      sourceType: Type,
      targetType: Type,
      strategy: FactDiscoveryStrategy,
      schema: Schema
   ): List<String>? {
      val sourceTaxiType: lang.taxi.types.Type = sourceType.taxiType
      val targetTaxiType: lang.taxi.types.Type = targetType.taxiType

      if (sourceTaxiType.isAssignableTo(targetTaxiType)) {
         return listOf(ReadSelf.buildInstruction())
      }
      return when (sourceTaxiType) {
         is PrimitiveType -> null // we can't search for property paths against something that's a Primitive type
         is UnionType -> null // union types don't exist as models, so no point in searching
         is EnumType -> {
            if (targetTaxiType is EnumType) {
               // This is a special placeholder we use to signal later
               // that we should use a ReadEnumSynonym navigator for the matching synonym type
               // Only created if the encountered enum has a synonym to the requested type
               val readEnumSynonymValueInstruction = sourceType.enumTypedInstances
                  .flatMap { enumValues -> enumValues.synonyms.map { synonymValue -> synonymValue.type } }
                  .firstOrNull { it.taxiType == targetTaxiType }
                  ?.let { listOf(ReadEnumSynonym.buildInstruction(it)) }
               readEnumSynonymValueInstruction
            } else {
               null // Synonyms can't work, as it's not Enum -> Enum, so nothing else to search
            }

         }

         is ObjectType -> sourceTaxiType.getDescendantPathsOfType(targetTaxiType)
         is ArrayType -> {

            val targetIsArray = targetTaxiType is ArrayType
            val isValidToSearch = when {
               // fail fast if the strategy means that traversal is pointless
               // ie., if the caller has requested a single value, that value isn't an array,
               // but the path sends us iterating through an array, then it can't succeed
               // so no point searching
               strategy == FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE && !targetIsArray -> false
               strategy == FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT && !targetIsArray -> false
               else -> true
            }
            if (!isValidToSearch) {
               logger.info { "Not searching from ${sourceType.qualifiedName.shortDisplayName} tp ${targetType.qualifiedName.shortDisplayName} with strategy of $strategy as the path encoutners an array, making the search invalid" }
               return null
            }
            getPath(schema.type(sourceTaxiType.memberType), schema.type(targetTaxiType), strategy, schema)
         }

         else -> error("Cannot search along type of ${sourceTaxiType.toQualifiedName().parameterizedName} which is of kind ${sourceTaxiType::class.simpleName}")
      }
   }

   private fun unwrapCollectionType(lastType: Type): Type {
      return if (!lastType.isCollection) {
         lastType
      } else {
         unwrapCollectionType(lastType.collectionType!!)
      }
   }
}

/**
 * Responsible for navigating a single element within a path provided by
 * PathTraversingFactSearcher.
 *
 * Implementations MUST be fast, as this code sits on the "hot path" of everything we do.
 */
interface TypeAttributeNavigator {
   fun navigate(
      previous: List<TypedInstance>,
      strategy: FactDiscoveryStrategy,
   ): List<TypedInstance>?
}

private class CompositePathAttributeNavigator(
   private val path: String,
   private val navigators: List<TypeAttributeNavigator>
) : TypeAttributeNavigator {
   override fun navigate(
      previous: List<TypedInstance>,
      strategy: FactDiscoveryStrategy
   ): List<TypedInstance>? {
      val finalEvaluation = this.navigators.fold(previous) { acc, typeNavigator ->
         val evaluated = typeNavigator.navigate(acc, strategy) ?: return null
         evaluated
      }
      return finalEvaluation

   }
}

private class ReadEnumSynonym(private val synonymType: Type) : TypeAttributeNavigator {
   companion object {
      private const val PREFIX = "~SynonymOf:"
      fun buildInstruction(enumType: Type): String {
         // The code that consumes this special instruction token expects to receive
         // a path of field names.
         // So, using dots (as found in package names) causes issues
         // Therefore, here we replace dots in the package with !
         // Below, we swap them back to look up the type
         return "$PREFIX${enumType.paramaterizedName.replace(".", "!")}"
      }

      fun isInstructionToken(string: String): Boolean = string.startsWith(PREFIX)
      fun fromInstruction(instruction: String, schema: Schema): ReadEnumSynonym {
         val enumName = instruction.removePrefix(PREFIX).replace("!", ".")
         return ReadEnumSynonym(schema.type(enumName))
      }
   }

   override fun navigate(previous: List<TypedInstance>, strategy: FactDiscoveryStrategy): List<TypedInstance>? {
      val synonyms = previous.mapNotNull { enumValue ->
         require(enumValue is TypedEnumValue) { "£xpected to receive an TypedEnumValue, but got an instance of ${enumValue::class.simpleName}" }
         val synonym = enumValue.synonyms.firstOrNull { it.type == this.synonymType }
         synonym
      }
      return synonyms.ifEmpty {
         null
      }
   }
}

private class ReadField(private val fieldName: String) : TypeAttributeNavigator {
   override fun navigate(
      previous: List<TypedInstance>,
      strategy: FactDiscoveryStrategy
   ): List<TypedInstance>? {
      return previous.map { instance ->
         require(instance is TypedObject) { "cannot navigate field $fieldName as was passed a ${instance::class.simpleName}" }
         // If the source object didn't provide a field (rather than providing it as null, which becomes a TypedNull),
         // then return null.
         // Note - I'm using the behaviour as it matches the legacy behaviour at the time of implementation.
         // However, a more correct behaviour here may be to return TypedNull
         if (!instance.hasAttribute(fieldName)) {
            return null
         } else {
            instance.getDirectAttribute(fieldName)
         }

      }
   }

}

private class IterateArray(private val fieldName: String) : TypeAttributeNavigator {
   override fun navigate(
      previous: List<TypedInstance>,
      strategy: FactDiscoveryStrategy
   ): List<TypedInstance>? {
      return previous
         .filter { it !is TypedNull }
         .flatMap { instance ->
            require(instance is TypedObject) { "cannot iterate field $fieldName as was passed a ${instance::class.simpleName}" }
            val fieldValue = instance[fieldName]
            require(fieldValue is TypedCollection) { "cannot iterate field $fieldName on type ${instance.type.qualifiedName.shortDisplayName} as it has type ${fieldValue.type.name.shortDisplayName} which is not iterable" }
            fieldValue
         }
   }
}

private object IterateSelf : TypeAttributeNavigator {
   override fun navigate(previous: List<TypedInstance>, strategy: FactDiscoveryStrategy): List<TypedInstance>? {
      require(previous.all { it is TypedCollection }) { "Cannot iterate self as root node is not a collection of TypedCollections" }
      return previous.flatMap { it as TypedCollection }
   }

}

private object ReadSelf : TypeAttributeNavigator {
   private const val PREFIX = "~Self"
   fun buildInstruction(): String {
      return PREFIX
   }

   fun isInstructionToken(string: String): Boolean = string == PREFIX
   override fun navigate(
      previous: List<TypedInstance>,
      strategy: FactDiscoveryStrategy
   ): List<TypedInstance> {
      return previous
   }
}


/**
 * Encapsulates a previously built path of how to get from a source type
 * to a target type by reading the attributes.
 *
 * For a given problem of A -> B, multiple paths may exist.
 * Each path is represented as a TypeAttributeNavigator within the navigators property.
 *
 * All paths are evaluated and collected together.
 * How the results are returned is determined by the FactDiscoveryStrategy present
 * on the provided FactSearch.
 * That may result in returning null, even when values were found (for example, if multiple
 * matches were found, but the provided strategy was ANY_DEPTH_EXPECT_ONE)
 *
 * Contains the actual navigators (not just the path as a string), so
 * for a given start instance, can evaluate the path and return the result of
 * the path
 */
data class NavigatableTypeAttributePath(
   val key: TypeTraversalKey,
   private val navigators: List<TypeAttributeNavigator>,
   private val targetType: Type
) {
   fun navigate(
      source: TypedInstance,
      search: FactSearch
   ): TypedInstance? {
      val collected = navigators.mapNotNull { pathNavigator ->
         val navigationResult = pathNavigator.navigate(listOf(source), search.strategy)
         // Resolve any enum synonyms that were returned, by converting back to the requested type
         val withResolvedSynonyms = navigationResult?.mapNotNull { typedInstance ->
            if (typedInstance is TypedEnumValue && typedInstance.type != targetType) {
               val synonym = typedInstance.synonyms.firstOrNull { synonym -> synonym.type.isAssignableTo(targetType) }
               synonym
            } else {
               typedInstance
            }
         }
         val filteredBySearchPredicate = withResolvedSynonyms?.filter { search.filterPredicate.predicate(it) }
         val finalNavigationResult =
            filteredBySearchPredicate?.let { result -> search.strategy.applyToResults(result, search) }
         finalNavigationResult
      }

      // This is different from the original implementation
      // Since there are potentially multiple paths to navigate,
      // we apply the search strategy filter again here.
      val finalResults = search.strategy.applyToResults(collected, search)
      return finalResults
   }
}

/**
 * This is a fast search implementation that will look across a group of facts and determine
 * if one-or-many facts of a provided type are present within the provided values.
 *
 * Rather than performing a search (which is what the old implementation used to do), this
 * uses the schema to build a set of paths from the provided type to the target type.
 *
 * These paths are then converted into PathTraverser
 */
class PathTraversingFactSearcher(private val searchCache: TypeTraversalPathCache = GlobalSchemaFactSearchCache) {
   fun getFact(
      facts: List<TypedInstance>,
      type: Type,
      schema: Schema,
      strategy: FactDiscoveryStrategy,
      spec: TypedInstanceValidPredicate
   ): TypedInstance? {
      val search = FactSearch.findType(type, strategy, spec)
      return getFact(search, facts, schema)
   }

   fun getFact(search: FactSearch, facts: List<TypedInstance>, schema: Schema): TypedInstance? {
      if (search.strategy == FactDiscoveryStrategy.TOP_LEVEL_ONLY) {
         return searchTopLevelFactsOnly(search, facts, schema)
      }
      val result = facts.mapNotNull { sourceFact ->
         val sourceType = sourceFact.type
         val traversalPath = searchCache.getTypeTraversal(
            schema,
            sourceType,
            search.targetType,
            search.strategy
         ) ?: return@mapNotNull null
         traversalPath.navigate(sourceFact, search)
      }.distinct()
      return search.strategy.applyToResults(result, search)
   }

   private fun searchTopLevelFactsOnly(search: FactSearch, facts: List<TypedInstance>, schema: Schema): TypedInstance? {
      return facts.firstOrNull { search.filterPredicate.predicate(it) }
   }
}
