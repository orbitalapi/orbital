package io.vyne.models.facts

import com.diffplug.common.base.TreeDef
import com.diffplug.common.base.TreeStream
import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Stopwatch
import io.vyne.models.*
import io.vyne.query.AlwaysGoodSpec
import io.vyne.query.TypedInstanceValidPredicate
import io.vyne.schemas.*
import io.vyne.utils.ImmutableEquality
import io.vyne.utils.cached
import lang.taxi.types.PrimitiveType
import mu.KotlinLogging
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.streams.toList


open class CopyOnWriteFactBag(
   private val facts: CopyOnWriteArrayList<TypedInstance>,
   private val schema: Schema
) : FactBag, Collection<TypedInstance> by facts {
   private val logger = KotlinLogging.logger {}

   constructor(facts: Collection<TypedInstance>, schema: Schema) : this(CopyOnWriteArrayList(facts), schema)
   constructor(fact: TypedInstance, schema: Schema) : this(listOf(fact), schema)

   open fun copy(): CopyOnWriteFactBag {
      return CopyOnWriteFactBag(facts, schema)
   }

   override fun merge(other: FactBag): FactBag {
      return CopyOnWriteFactBag(
         this.facts + other.toList(),
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
      this.modelTree.invalidate()
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

   private val anyArrayType by lazy { schema.type(PrimitiveType.ANY) }

   // Wraps all the known facts under a root node, turning it into a tree
   private fun dataTreeRoot(): TypedInstance {
      return TypedCollection.arrayOf(anyArrayType, facts.toList(), source = MixedSources)
   }

   private val modelTree = cached<List<TypedInstance>> {
      val navigator = TreeNavigator()
      val treeDef: TreeDef<TypedInstance> = TreeDef.of { instance -> navigator.visit(instance) }
      val list = TreeStream.breadthFirst(treeDef, dataTreeRoot()).toList()
      list
   }

   override fun breadthFirstFilter(
      strategy: FactDiscoveryStrategy,
      predicate: (TypedInstance) -> Boolean
   ): List<TypedInstance> {
      return modelTree().filter(predicate)
   }

   /**
    * A breadth-first stream of data facts currently held in the collection.
    * Use breadth-first, as we want to favour nodes closer to the root.
    * Deeply nested children are less likely to be relevant matches.
    */
   private fun modelTree(): List<TypedInstance> {
      // TODO : MP - Investigating the performance implications of caching the tree.
      // If this turns out to be faster, we should refactor the api to be List<TypedInstance>, since
      // the stream indicates deferred evaluation, and it's not anymore.
      return modelTree.get()
   }

   private data class GetFactOrNullCacheKey(
      val search: FactSearch
   ) {
      private val equality = ImmutableEquality(
         this,
         GetFactOrNullCacheKey::search,
      )

      override fun equals(other: Any?): Boolean {
         return equality.isEqualTo(other)
      }

      override fun hashCode(): Int {
         return equality.hash()
      }
   }


   override fun hasFactOfType(
      type: Type,
      strategy: FactDiscoveryStrategy,
      spec: TypedInstanceValidPredicate
   ): Boolean {
      // This could be optimized, as we're searching twice for everything, and not caching anything
      return getFactOrNull(type, strategy, spec) != null
   }

   override fun getFact(
      type: Type,
      strategy: FactDiscoveryStrategy,
      spec: TypedInstanceValidPredicate
   ): TypedInstance {
      // This could be optimized, as we're searching twice for everything, and not caching anything
      return getFactOrNull(type, strategy, spec)!!
   }


   /**
    * getFactOrNull is called frequently, and can generate a VERY LARGE call stack.  In some profiler passes, we've
    * seen 40k calls to getFactOrNull, which in turn generates a call stack with over 18M invocations.
    * So, cache the calls.
    */
   private val factSearchCache = ConcurrentHashMap<GetFactOrNullCacheKey, Optional<TypedInstance>>()
   private fun fromFactCache(key: GetFactOrNullCacheKey): TypedInstance? {
//      val stopwatch = Stopwatch.createStarted()
//      val allFacts = breadthFirstFilter(FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY) { true }
//      val groupedByType = allFacts.groupBy { it.type.fullyQualifiedName }
//      logger.debug { "Flattened to ${allFacts.size} facts (grouped to ${groupedByType.keys.size} types) in ${stopwatch.elapsed().toMillis()}ms" }


      val optionalVal = factSearchCache.getOrPut(key) {
//         val stopwatch = Stopwatch.createStarted()
         val result = Optional.ofNullable(key.search.strategy.getFact(this, key.search))
//         val found = if (result.isPresent) "DID" else "did NOT"
//         logger.debug { "CopyOnWriteFactBag search ${key.search.name} took ${stopwatch.elapsed().toMillis()}ms and $found return a result" }
         result
      }
      return if (optionalVal.isPresent) optionalVal.get() else null
   }

   override fun getFactOrNull(
      type: Type,
      strategy: FactDiscoveryStrategy,
      spec: TypedInstanceValidPredicate
   ): TypedInstance? {
      val searchCacheKey = getFactOrNullCacheKey(strategy, type, spec)


      return fromFactCache(searchCacheKey)
   }

   private fun getFactOrNullCacheKey(
      strategy: FactDiscoveryStrategy,
      type: Type,
      spec: TypedInstanceValidPredicate
   ): GetFactOrNullCacheKey {
      // MP 4-Nov-22
      // Design choice around searching for arrays: (weakly held):
      // Sometimes when we're searching for Foo[], we want to gather up all the values.
      // (ie., traverse an object graph, and collect all the instances of Foo).
      // Other times, we only want to find exact instances of Foo[]
      // We're using the  ALLOW_MANY / ALLOW_ONE heuristic to determine how we search.
      // Not sure this is correct.  See CopyOnWriteFactBagTest for tests that explore this with use-cases.
      val predicate = if (strategy == FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY) {
         TypeMatchingStrategy.MATCH_ON_COLLECTION_TYPE.or(TypeMatchingStrategy.ALLOW_INHERITED_TYPES)
      } else {
         TypeMatchingStrategy.ALLOW_INHERITED_TYPES // default
      }
      return GetFactOrNullCacheKey(FactSearch.findType(type, strategy, spec, predicate))
   }

   @VisibleForTesting
   internal fun searchIsCached(
      type: Type,
      strategy: FactDiscoveryStrategy = FactDiscoveryStrategy.TOP_LEVEL_ONLY,
      spec: TypedInstanceValidPredicate = AlwaysGoodSpec
   ): Boolean {
      val key = getFactOrNullCacheKey(strategy, type, spec)
      return factSearchCache.containsKey(key)
   }


   override fun getFactOrNull(
      search: FactSearch,
   ): TypedInstance? {
      return fromFactCache(GetFactOrNullCacheKey(search))
   }

   override fun hasFact(
      search: FactSearch
   ): Boolean {
      return getFactOrNull(search) != null
   }


}

private class TreeNavigator {
   private val visitedNodes = mutableSetOf<TypedInstance>()

   fun visit(instance: TypedInstance): List<TypedInstance> {
      return if (visitedNodes.contains(instance)) {
         return emptyList()
      } else {
         visitedNodes.add(instance)
         TypedInstanceTree.visit(instance)
      }
   }
}


private object TypedInstanceTree {
   /**
    * Function which defines how to convert a TypedInstance into a tree, for traversal
    */

   fun visit(instance: TypedInstance): List<TypedInstance> {

      if (instance.type.isClosed) {
         return emptyList()
      }

      return when (instance) {
         is TypedObject -> instance.values.toList()
         is TypedEnumValue -> {
            instance.synonyms
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
         is TypedNull -> emptyList()
         else -> throw IllegalStateException("TypedInstance of type ${instance.javaClass.simpleName} is not handled")
      }
   }
}

