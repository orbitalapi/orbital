package com.orbitalhq.models.facts

import com.diffplug.common.base.TreeDef
import com.diffplug.common.base.TreeStream
import com.google.common.annotations.VisibleForTesting
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.orbitalhq.models.MixedSources
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedEnumValue
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.TypedValue
import com.orbitalhq.query.AlwaysGoodSpec
import com.orbitalhq.query.TypedInstanceValidPredicate
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.utils.timeBucket
import lang.taxi.types.PrimitiveType
import mu.KotlinLogging
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Collectors

private val logger = KotlinLogging.logger {}
open class CopyOnWriteFactBag(
   private val facts: CopyOnWriteArrayList<TypedInstance>,
   override val scopedFacts: List<ScopedFact>,
   private val schema: Schema
) : FactBag {
   companion object {
      internal val useExperimentalFactSearch: Boolean = false // System.getProperty("ORBITAL_FAST_SEARCH_ENABLED") == "true"
      private val factSearcher: PathTraversingFactSearcher = PathTraversingFactSearcher(GlobalSchemaFactSearchCache)
   }



   constructor(facts: Collection<TypedInstance>, schema: Schema, scopedFacts: List<ScopedFact> = emptyList()) : this(
      CopyOnWriteArrayList(facts),
      scopedFacts,
      schema
   )

   constructor(fact: TypedInstance, schema: Schema) : this(listOf(fact), schema)

   override fun rootFacts(): List<TypedInstance> {
      // Exclude scopedFacts. They're included by calling rootAndScopedFacts()
      return facts
   }

   open fun copy(): CopyOnWriteFactBag {
      return CopyOnWriteFactBag(facts, schema)
   }

   override fun merge(other: FactBag): FactBag {
      return CopyOnWriteFactBag(
         CopyOnWriteArrayList(this.facts + other.toList()),
         this.scopedFacts + other.scopedFacts,
         schema
      )
   }

   override fun merge(fact: TypedInstance): FactBag {
      return copy().addFact(fact)
   }

   override fun excluding(facts: Set<TypedInstance>): FactBag {
      val copy = copy()
      copy.facts.removeAll(facts.toSet())
      return copy
   }

   override val size: Int
      get() {
         return facts.size
      }

   override fun addFact(fact: TypedInstance): CopyOnWriteFactBag {
      this.facts.add(fact)
      this.modelTreeCache.invalidateAll()
      this.cachedRootAndScopedFacts.set(null)
      // Now that we have a new fact, invalidate queries where we had asked for a fact
      // previously, and had returned null.
      // This allows new queries to discover new values.
      // All other getFactOrNull() calls will retain cached values.
      removeNullsFromFactSearchCache()
      return this
   }

   private fun removeNullsFromFactSearchCache() {
      val keysToRemove = this.factSearchCache.mapNotNull { (key, value) ->
         val shouldRemove = value != null
         if (shouldRemove) {
            key
         } else {
            null
         }
      }
      keysToRemove.forEach { this.factSearchCache.remove(it) }
   }

   override fun addFacts(facts: Collection<TypedInstance>): CopyOnWriteFactBag {
      facts.forEach { this.addFact(it) }
      return this
   }

   private val cachedRootAndScopedFacts = AtomicReference<List<TypedInstance>?>(null)
   override fun rootAndScopedFacts(): List<TypedInstance> {
      return cachedRootAndScopedFacts.updateAndGet { value ->
         value ?: (rootFacts() + scopedFacts.map { it.fact })
      }!!
   }

   private val anyArrayType by lazy { schema.type(PrimitiveType.ANY) }

   // Wraps all the known facts under a root node, turning it into a tree
   private fun dataTreeRoot(): TypedInstance {
      // MP: 17-Nov-22
      // This used to only be root facts.
      // But that was causing lookups-by-type to incorrectly ignore things that have been scoped
      // (eg., when projecting an object, the projected element is scoped, and ignored for lookup-by-type)
      return TypedCollection.arrayOf(anyArrayType, rootAndScopedFacts().distinct(), source = MixedSources)
   }

   private val modelTreeCache = CacheBuilder
      .newBuilder()
      .build(object : CacheLoader<FactMapTraversalStrategy, List<TypedInstance>>() {
         override fun load(key: FactMapTraversalStrategy): List<TypedInstance> {
            val navigator = TreeNavigator(key.predicate)
            val treeDef: TreeDef<TypedInstance> = TreeDef.of { instance -> navigator.visit(instance) }
            val list = TreeStream.breadthFirst(treeDef, dataTreeRoot())
               .collect(Collectors.toList())
            return list
         }

      })

//   private val modelTree = cached<List<TypedInstance>> {
//      val navigator = TreeNavigator()
//      val treeDef: TreeDef<TypedInstance> = TreeDef.of { instance -> navigator.visit(instance) }
//      val list = TreeStream.breadthFirst(treeDef, dataTreeRoot()).toList()
//      list
//   }

   override fun breadthFirstFilter(
      strategy: FactDiscoveryStrategy,
      shouldGoDeeperPredicate: FactMapTraversalStrategy,
      matchingPredicate: (TypedInstance) -> Boolean
   ): List<TypedInstance> {
      val instancesToEvaluate = modelTree(shouldGoDeeperPredicate)
      val filtered = instancesToEvaluate.filter(matchingPredicate)
      return filtered
   }

   /**
    * A breadth-first stream of data facts currently held in the collection.
    * Use breadth-first, as we want to favour nodes closer to the root.
    * Deeply nested children are less likely to be relevant matches.
    */
   private fun modelTree(shouldGoDeeperPredicate: FactMapTraversalStrategy): List<TypedInstance> {
      val modelTree = modelTreeCache.get(shouldGoDeeperPredicate)
      return modelTree
   }

   private data class GetFactOrNullCacheKey(
      val search: FactSearch
   ) {
      private val cachedHashCode: Int = search.hashCode()

      override fun equals(other: Any?): Boolean {
         if (this === other) return true
         if (other !is GetFactOrNullCacheKey) return false

         return search == other.search
      }

      override fun hashCode(): Int {
         return cachedHashCode
      }
   }


   override fun hasFactOfType(
      type: Type,
      strategy: FactDiscoveryStrategy,
      spec: TypedInstanceValidPredicate
   ): Boolean {
      return getFactOrNull(type, strategy, spec) != null
   }

   /**
    * Experimental method that uses new searching algorithim.
    * Intent is to replace the existing getFactOrNull with this approach.
    * However, for the time being, this sits behind a feature toggle
    */
   // Working title.
   // This should replace getFact if I can prove it's faster
   fun getFactFast(
      type: Type,
      strategy: FactDiscoveryStrategy = FactDiscoveryStrategy.TOP_LEVEL_ONLY,
      spec: TypedInstanceValidPredicate = AlwaysGoodSpec
   ): TypedInstance {
      return getFactOrNullFast(type, strategy, spec)
         ?: error("Failed to resolve type ${type.name.shortDisplayName} using strategy $strategy")
   }

   override fun getFact(
      type: Type,
      strategy: FactDiscoveryStrategy,
      spec: TypedInstanceValidPredicate
   ): TypedInstance {
      return if (useExperimentalFactSearch) {
         getFactFast(type, strategy, spec)
      } else {
         getFactOrNull(type, strategy, spec)
            ?: error("Failed to resolve type ${type.name.shortDisplayName} using strategy $strategy")
      }
   }


   /**
    * getFactOrNull is called frequently, and can generate a VERY LARGE call stack.  In some profiler passes, we've
    * seen 40k calls to getFactOrNull, which in turn generates a call stack with over 18M invocations.
    * So, cache the calls.
    */
   private val factSearchCache = ConcurrentHashMap<GetFactOrNullCacheKey, Optional<TypedInstance>>()
   private fun fromFactCache(key: GetFactOrNullCacheKey): TypedInstance? {
      val optionalVal = factSearchCache.getOrPut(key) {
         val result = timeBucket("FactBag search for ${key.search.name}") {
            Optional.ofNullable(
               key.search.strategy.getFact(
                  this,
                  key.search
               )
            )
         }
         result
      }
      return if (optionalVal.isPresent) optionalVal.get() else null
   }

   override fun getFactOrNull(
      type: Type,
      strategy: FactDiscoveryStrategy,
      spec: TypedInstanceValidPredicate
   ): TypedInstance? {
      logger.trace { "Searching for type ${type.name.shortDisplayName}" }
      val f =  if (useExperimentalFactSearch) {
         getFactOrNullFast(type, strategy, spec)
      } else {
         val search = FactSearch.findType(type, strategy, spec)
         val searchCacheKey = getFactOrNullCacheKey(search)
         val result = fromFactCache(searchCacheKey)
         result
      }
      logger.trace { "Search for type returned ${f?.type?.name?.shortDisplayName}" }
      return f
   }

   /**
    * Experimental method that uses new searching algorithim.
    * Intent is to replace the existing getFactOrNull with this approach.
    * However, for the time being, this sits behind a feature toggle
    */
   fun getFactOrNullFast(
      type: Type,
      strategy: FactDiscoveryStrategy,
      spec: TypedInstanceValidPredicate
   ): TypedInstance? {
      return factSearcher.getFact(this.rootAndScopedFacts(), type, this.schema, strategy, spec)
   }

   private fun getFactOrNullCacheKey(
      factSearch: FactSearch
   ): GetFactOrNullCacheKey {
      return GetFactOrNullCacheKey(factSearch)
   }

   @VisibleForTesting
   internal fun searchIsCached(
      type: Type,
      strategy: FactDiscoveryStrategy = FactDiscoveryStrategy.TOP_LEVEL_ONLY,
      spec: TypedInstanceValidPredicate = AlwaysGoodSpec
   ): Boolean {
      val key = getFactOrNullCacheKey(FactSearch.findType(type, strategy, spec))
      return factSearchCache.containsKey(key)
   }


   override fun getFactOrNull(
      search: FactSearch,
   ): TypedInstance? {
      logger.trace { "Search: $search" }
      val f = when (search.searchAlgorithm) {
         // This is the preferred approach, but is newer so toggled-off
         // by default.
         FactSearch.SearchAlgorithm.AttributeNavigation -> {
            return if (useExperimentalFactSearch) {
               factSearcher.getFact(search, this.rootAndScopedFacts(), schema)
            } else {
               fromFactCache(GetFactOrNullCacheKey(search))
            }
         }
         FactSearch.SearchAlgorithm.TreeSearch -> {
            fromFactCache(GetFactOrNullCacheKey(search))
         }
      }
      logger.trace { "Search returned ${f?.type?.name?.shortDisplayName}" }
      return f
   }

   override fun hasFact(
      search: FactSearch
   ): Boolean {
      return getFactOrNull(search) != null
   }

   override fun withAdditionalScopedFacts(otherFacts: List<ScopedFact>, schema: Schema): FactBag {
      return CopyOnWriteFactBag(
         facts, schema, scopedFacts + otherFacts
      )
   }

   override fun withAdditionalFacts(otherFacts: List<TypedInstance>, schema: Schema): FactBag {
      return CopyOnWriteFactBag(
         facts + otherFacts, schema, scopedFacts
      )
   }

}

private class TreeNavigator(val shouldGoDeeperPredicate: (TypedInstance) -> TreeNavigationInstruction) {
   private val visitedNodes = mutableSetOf<TypedInstance>()

   fun visit(instance: TypedInstance): List<TypedInstance> {
      return if (visitedNodes.contains(instance)) {
         return emptyList()
      } else {
         visitedNodes.add(instance)
         val next = TypedInstanceTree.visit(instance, shouldGoDeeperPredicate)
         next
      }
   }
}


private object TypedInstanceTree {
   /**
    * Function which defines how to convert a TypedInstance into a tree, for traversal
    */

   fun visit(
      instance: TypedInstance,
      navigationPredicate: (TypedInstance) -> TreeNavigationInstruction
   ): List<TypedInstance> {

      // We've changed the semantics of closed.
      // It now means "don't attempt to construct this from other things",
      // where it used to mean "Don't attempt to deconstruct this".
      // We need a new term for that - probably sealed
//      if (instance.type.isClosed) {
//         return emptyList()
//      }
      val navigationInstruction = navigationPredicate(instance)
      if (navigationInstruction == IgnoreThisElement) {
         return emptyList()
      }

      return when (instance) {
         is TypedObject -> {
            when (navigationInstruction) {
               is FullScan -> instance.values.toList()
               is EvaluateSpecificFields -> navigationInstruction.filter(instance)
               else -> error("Unhandled branch for navigation instructionof type ${navigationInstruction::class.simpleName}")
            }

         }

         is TypedEnumValue -> {
            // MP 10-Oct-24: Support enum value objects
            if (instance.enumValue.value is TypedObject) {
               listOf(instance.enumValue.value) + instance.synonyms
            } else {
               instance.synonyms
            }

         }

         is TypedValue -> {
            if (instance.type.isEnum) {
               error("EnumSynonyms as TypedValue not supported here")
//               EnumSynonyms.fromTypeValue(instance)
            } else {
               emptyList()
            }

         }

         is TypedCollection -> instance.value
         is TypedNull -> {
            when (navigationInstruction) {
               is FullScan -> {
                  // Not sure what to do here. Guide me, o unit tests.
                  // This call may not be correct. We don't hit this in any unit tests.
                  navigationInstruction.returnNulls(instance)
               }
               is EvaluateSpecificFields -> {
                  navigationInstruction.returnNulls(instance)
               }
               else -> emptyList()
            }
         }
         else -> throw IllegalStateException("TypedInstance of type ${instance.javaClass.simpleName} is not handled")
      }
   }
}

