package io.vyne.query

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import es.usc.citius.hipster.algorithm.Algorithm
import es.usc.citius.hipster.graph.HipsterDirectedGraph
import es.usc.citius.hipster.model.impl.WeightedNode
import io.vyne.VyneCacheConfiguration
import io.vyne.models.TypedInstance
import io.vyne.query.graph.*
import io.vyne.schemas.Link
import io.vyne.schemas.Path
import io.vyne.schemas.Relationship
import io.vyne.schemas.Schema
import io.vyne.schemas.describe
import io.vyne.utils.log
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import lang.taxi.Equality

class EdgeNavigator(linkEvaluators: List<EdgeEvaluator>) {
   private val evaluators = linkEvaluators.associateBy { it.relationship }

   suspend fun evaluate(edge: EvaluatableEdge, queryContext: QueryContext): EvaluatedEdge {
      val relationship = edge.relationship
      val evaluator = evaluators[relationship]
         ?: error("No LinkEvaluator provided for relationship ${relationship.name}")
      val evaluationResult = if (queryContext.debugProfiling) {

         //TODO
         //queryContext.startChild(this, "Evaluating ${edge.description} with evaluator ${evaluator.javaClass.simpleName}", OperationType.GRAPH_TRAVERSAL) {
         evaluator.evaluate(edge, queryContext)
         //}

      } else {
         evaluator.evaluate(edge, queryContext)
      }
      return evaluationResult
   }
}

class SearchPathExclusionsMap<K,V>(private val maxEntries: Int): LinkedHashMap<K, V>() {
   override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
      return this.size > maxEntries
   }
}

class HipsterDiscoverGraphQueryStrategy(
   private val edgeEvaluator: EdgeNavigator,
   vyneCacheConfigration: VyneCacheConfiguration) : QueryStrategy {
   private val searchPathExclusionsCacheSize = vyneCacheConfigration.vyneDiscoverGraphQuery.searchPathExclusionsCacheSize

   private val schemaGraphCache = CacheBuilder.newBuilder()
      .maximumSize(vyneCacheConfigration.vyneDiscoverGraphQuery.schemaGraphCacheSize) // arbitary cache size, we can explore tuning this later
      .weakKeys()
      .build(object : CacheLoader<Schema, VyneGraphBuilder>() {
         override fun load(schema: Schema): VyneGraphBuilder {
            return VyneGraphBuilder(schema, vyneCacheConfigration.vyneGraphBuilderCache)
         }

      })

   private val searchPathExclusions = SearchPathExclusionsMap<SearchPathExclusionKey, SearchPathExclusionKey>(searchPathExclusionsCacheSize)
   data class SearchPathExclusionKey(val startInstance: TypedInstance, val target: Element) {
      private val equality = Equality(this, SearchPathExclusionKey::startInstance, SearchPathExclusionKey::target)
      private val hash:Int by lazy { equality.hash() }
      override fun equals(other: Any?): Boolean = equality.isEqualTo(other)
      override fun hashCode(): Int = hash
   }



   override suspend fun invoke(target: Set<QuerySpecTypeNode>, context: QueryContext, invocationConstraints: InvocationConstraints): QueryStrategyResult {

      return find(target, context, invocationConstraints)
   }

   fun find(targets: Set<QuerySpecTypeNode>, context: QueryContext, invocationConstraints: InvocationConstraints): QueryStrategyResult {
      // Note : There is an existing, working impl. of this in QueryEngine (the OrientDB approach),
      // but I haven't gotten around to copying it yet.
      if (targets.size != 1) TODO("Support for target sets not yet built")
      val target = targets.first()

      // We only support DISCOVER_ONE mode here.
      if (target.mode != QueryMode.DISCOVER) return QueryStrategyResult.empty()

      if (context.facts.isEmpty()) {
         log().info("Cannot perform a graph search, as no facts provied to serve as starting point. ")
         return QueryStrategyResult.empty()
      }

      val targetElement = type(target.type)

      // search from every fact in the context
      val lastResult: TypedInstance? = find(targetElement, context, invocationConstraints)
      return if (lastResult != null) {
         QueryStrategyResult( flow { mapOf(target to lastResult).map { it.value } })
      } else {
         QueryStrategyResult.empty()
      }
   }

   internal fun find(targetElement: Element, context: QueryContext, invocationConstraints: InvocationConstraints):TypedInstance? {
      // Take a copy, as the set is mutable, and performing a search is a
      // mutating operation, discovering new facts, which can lead to a ConcurrentModificationException
      val currentFacts = context.facts.toSet()
      return  currentFacts
         .asSequence()
     //    .filter { it is TypedObject }
         .mapNotNull { fact ->
            val startFact =  providedInstance(fact)
            val targetType = context.schema.type(targetElement.value as String)
            val exclusionKey = SearchPathExclusionKey(fact, targetElement)
            if (searchPathExclusionsCacheSize > 0 && searchPathExclusions.contains(exclusionKey)) {
               // if  a previous search for given (searchNode, targetNode) yielded 'null' path, then
               // don't search.
               return@mapNotNull null
            }
            val searcher = GraphSearcher(startFact, targetElement, targetType, schemaGraphCache.get(context.schema), invocationConstraints)
            val searchResult = searcher.search(
               currentFacts,
               context.excludedServices.toSet(),
               invocationConstraints.excludedOperations.plus(context.excludedOperations.map { SearchGraphExclusion("@Id", it) })) { pathToEvaluate ->
               evaluatePath(pathToEvaluate,context)
            }
            if (searchPathExclusionsCacheSize > 0 && searchResult.path == null) {
               searchPathExclusions[exclusionKey] = exclusionKey
            }
            searchResult.typedInstance
         }
         .firstOrNull()
   }

   private fun evaluatePath(searchResult: WeightedNode<Relationship, Element, Double>, queryContext: QueryContext): List<PathEvaluation> {
      // The actual result of this isn't directly used.  But the queryContext is updated with
      // nodes as they're discovered (eg., through service invocation)
      val evaluatedEdges = mutableListOf<PathEvaluation>(
         getStartingEdge(searchResult, queryContext)
      )
      val path = searchResult.path()
      path
         .drop(1)
         .asSequence()
         .takeWhile {
            val lastEvaluation = evaluatedEdges.last()
            // Take as long as the last evaluation we made was successful.  Otherwise, stop.
            if (lastEvaluation is EvaluatedEdge) {
               lastEvaluation.wasSuccessful
            } else {
               true
            }
         }
         .mapIndexedTo(evaluatedEdges) { index, weightedNode ->
            // Note re index:  We dropped 1, so indexes are out-by-one.
            // Normally the lastValue would be index-1, but here, it's just index.
            val lastResult = evaluatedEdges[index]
            val endNode = weightedNode.state()
            val evaluatableEdge = EvaluatableEdge(lastResult, weightedNode.action(), endNode)
            if (evaluatableEdge.relationship == Relationship.PROVIDES) {
               log().info("As part of search ${path[0].state().value} -> ${path.last().state().value}, ${evaluatableEdge.vertex1.value} will be tried")
            }

            val evaluationResult = runBlocking {
                edgeEvaluator.evaluate(evaluatableEdge, queryContext)
            }

            if (evaluatableEdge.relationship == Relationship.PROVIDES) {
               log().info("As part of search ${path[0].state().value} -> ${path.last().state().value}, ${evaluatableEdge.vertex1.value} was executed. Successful : ${evaluationResult.wasSuccessful}")
            }
            evaluationResult
         }

      return evaluatedEdges
   }

   fun getStartingEdge(searchResult: WeightedNode<Relationship, Element, Double>, queryContext: QueryContext): StartingEdge {
      val firstNode = searchResult.path().first()
      if (firstNode.state().instanceValue is TypedInstance) {
         return StartingEdge(firstNode.state().instanceValue as TypedInstance, firstNode.state())
      }

      // Legacy -- is this still valid?  Why do we hit this, now we're adding typedInstances to
      // the graph?
      val firstType = queryContext.schema.type(firstNode.state().valueAsQualifiedName())
      val firstFact = queryContext.getFactOrNull(firstType)
      require(firstFact != null) { "The queryContext doesn't have a fact present of type ${firstType.fullyQualifiedName}, but this is the starting point of the discovered solution." }
      val startingEdge = StartingEdge(firstFact, firstNode.state())
      return startingEdge
   }
}

private fun List<WeightedNode<Relationship, Element, Double>>.toLinks(): List<Link> {
   return this.mapIndexed { index, _ ->
      if (index == 0) {
         null
      } else {
         val fromElement = this[index - 1].state()
         val toElement = this[index].state()
         val action = this[index].action()
         Link(fromElement.valueAsQualifiedName(), action, toElement.valueAsQualifiedName(), this[index].cost.toInt())
      }
   }.toList().filterNotNull()
}

private fun List<WeightedNode<Relationship, Element, Double>>.describe(): String {
   return this.toLinks().describe()
}

private fun List<WeightedNode<Relationship, Element, Double>>.describeLinks(): List<String> {
   return this.toLinks().map { it.toString() }
}

private fun Algorithm<*, Element, *>.SearchResult.recreatePath(start: Element, target: Element, graph: HipsterDirectedGraph<Element, Relationship>): Path {
   val path = this.getOptimalPaths()[0]
   val links = path.mapIndexed { index, vertex ->
      if (index + 1 >= path.size) {
         null
      } else {
         val fromElement = vertex
         val toElement = path[index + 1]
         val edge = graph.outgoingEdgesOf(fromElement).firstOrNull { it.vertex2 == toElement }
            ?: throw IllegalStateException("No edge found from $fromElement -> $toElement, but they were adjoining nodes in the result")
         Link(fromElement.valueAsQualifiedName(), edge.edgeValue, toElement.valueAsQualifiedName())
      }
   }.filterNotNull()
   return Path(start.valueAsQualifiedName(), target.valueAsQualifiedName(), links)
}

fun <V, E> HipsterDirectedGraph<V, E>.edgeDescriptions(): List<String> {
   return this.vertices().flatMap {
      this.outgoingEdgesOf(it).map { edge ->
         "${edge.vertex1} -[${edge.edgeValue}]-> ${edge.vertex2}"
      }
   }
}


