package io.vyne.query

import io.vyne.*
import io.vyne.models.TypedInstance
import io.vyne.query.graph.EvaluatedEdge
import io.vyne.query.graph.operationInvocation.SearchRuntimeException
import io.vyne.schemas.Schema
import io.vyne.utils.log


open class SearchFailedException(message: String, val evaluatedPath: List<EvaluatedEdge>, val profilerOperation: ProfilerOperation) : RuntimeException(message)

interface QueryEngine {

   val schema: Schema
   fun find(queryString: String, context: QueryContext): QueryResult
   fun find(target: QuerySpecTypeNode, context: QueryContext): QueryResult
   fun find(target: Set<QuerySpecTypeNode>, context: QueryContext): QueryResult

   fun gather(queryString: String, context: QueryContext): QueryResult

   fun queryContext(factSetIds: Set<FactSetId> = setOf(FactSets.DEFAULT), additionalFacts: Set<TypedInstance> = emptySet()): QueryContext

}

/**
 * A query engine which allows for the provision of initial state
 */
class StatefulQueryEngine(initialState: FactSetMap, schema: Schema, strategies: List<QueryStrategy>, private val profiler: QueryProfiler = QueryProfiler()) : BaseQueryEngine(schema, strategies), ModelContainer {
   private val factSets: FactSetMap = FactSetMap.create()

   init {
      factSets.putAll(initialState)
   }


//   override fun find(queryString: String, factSet: Set<TypedInstance>): QueryResult {
//      val nodeSetsWithLocalState = factSets.copy()
//      nodeSetsWithLocalState.putAll(FactSets.DEFAULT, factSet)
//
//      return super.find(queryString, nodeSetsWithLocalState.values().toSet())
//   }

   override fun addModel(model: TypedInstance, factSetId: FactSetId): StatefulQueryEngine {
      this.factSets[factSetId].add(model)
      return this
   }

   override fun queryContext(factSetIds: Set<FactSetId>, additionalFacts: Set<TypedInstance>): QueryContext {
      val facts = this.factSets.filterFactSets(factSetIds).values().toSet()
      return QueryContext.from(schema, facts + additionalFacts, this, profiler)
   }
}

// Note:  originally, there were two query engines (Default and Stateful), but only one was ever used (stateful).
// I've removed the default, and made it the BaseQueryEngine.  However, even this might be overkill, and we may
// fold this into a single class later.
// The seperation between what's in the base and whats in the concrete impl. is not well thought out currently.
abstract class BaseQueryEngine(override val schema: Schema, private val strategies: List<QueryStrategy>) : QueryEngine {

   private val queryParser = QueryParser(schema)
   override fun gather(queryString: String, context: QueryContext): QueryResult {
      // First pass impl.
      // Thinking here is that if I can add a new Hipster strategy that discovers all the
      // endpoints, then I can compose a result of gather() from multiple finds()
      val gatherQuery = queryParser.parse(queryString).map { it.copy(mode = QueryMode.GATHER) }.toSet()
      return find(gatherQuery, context)
   }

//   override fun queryContext(factSet: Set<TypedInstance>): QueryContext {
//      return QueryContext.from(schema, factSet, this, profiler)
//   }

   override fun find(queryString: String, context: QueryContext): QueryResult {
      val target = queryParser.parse(queryString)
      return find(target, context)
   }

   override fun find(target: QuerySpecTypeNode, context: QueryContext): QueryResult {
      return find(setOf(target), context)
   }

   override fun find(target: Set<QuerySpecTypeNode>, context: QueryContext): QueryResult {
      try {
         return doFind(target, context)
      } catch (e: Exception) {
         log().error("Search failed with exception:", e);
         throw SearchRuntimeException(e, context.profiler.root)
      }
   }

   private fun doFind(target: Set<QuerySpecTypeNode>, context: QueryContext): QueryResult {
      val matchedNodes = mutableMapOf<QuerySpecTypeNode, TypedInstance?>()
      // This is cheating, probably.
      // We only resolve top-level nodes, rather than traverse deeply.
      fun unresolvedNodes(): List<QuerySpecTypeNode> {
         return target.filterNot { matchedNodes.containsKey(it) }
      }

      val strategyIterator: Iterator<QueryStrategy> = strategies.iterator()
      while (strategyIterator.hasNext() && unresolvedNodes().isNotEmpty()) {
         val queryStrategy = strategyIterator.next()
         val strategyResult = context.startChild(this, "Query with ${queryStrategy.javaClass.simpleName}", OperationType.GRAPH_TRAVERSAL) { op ->
            op.addContext("Search target", target.map { it.type.fullyQualifiedName })
            queryStrategy.invoke(target, context)
         }
         // Note : We should add this additional data to the context too,
         // so that it's available for future query strategies to use.
         context.addFacts(strategyResult.matchedNodes.values.filterNotNull())

         matchedNodes.putAll(strategyResult.matchedNodes)

         if (strategyResult.additionalData.isNotEmpty()) {
            // Note: Maybe we should only start re-querying if unresolvedNodes() has content
            log().debug("Discovered additional facts, adding to the context")
            context.addFacts(strategyResult.additionalData)
         }
      }
      if (unresolvedNodes().isNotEmpty()) {
         log().error("The following nodes weren't matched: ${unresolvedNodes().joinToString(", ")}")
      }

      //      TODO("Rebuild Path")
      return QueryResult(matchedNodes, unresolvedNodes().toSet(), path = null, profilerOperation = context.profiler.root)
   }
}

