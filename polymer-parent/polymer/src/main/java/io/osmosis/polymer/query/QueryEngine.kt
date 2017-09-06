package io.osmosis.polymer.query

import io.osmosis.polymer.ModelContainer
import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.query.graph.EvaluatedEdge
import io.osmosis.polymer.schemas.Schema
import io.osmosis.polymer.utils.log


open class SearchFailedException(message:String, val evaluatedPath:List<EvaluatedEdge>) : RuntimeException(message)

interface QueryEngine {

   val schema: Schema
   fun find(queryString: String, factSet: Set<TypedInstance> = emptySet()): QueryResult
   fun find(target: QuerySpecTypeNode, factSet: Set<TypedInstance> = emptySet()): QueryResult
   fun find(target: Set<QuerySpecTypeNode>, factSet: Set<TypedInstance> = emptySet()): QueryResult
   fun find(target: Set<QuerySpecTypeNode>, context: QueryContext): QueryResult
   fun queryContext(factSet: Set<TypedInstance> = emptySet()): QueryContext
}

/**
 * An extension to the QueryEngine, allowing for the provision of initial state
 */
class StatefulQueryEngine(initialState: Set<TypedInstance>, private val queryEngine: QueryEngine) : QueryEngine by queryEngine, ModelContainer {
   private val models: MutableSet<TypedInstance> = initialState.toMutableSet()

   override fun find(queryString: String, factSet: Set<TypedInstance>): QueryResult {
      return queryEngine.find(queryString, factSet + models)
   }

   override fun addModel(model: TypedInstance): StatefulQueryEngine {
      this.models.add(model)
      return this
   }

   fun queryContext(): QueryContext {
      return queryEngine.queryContext(models)
   }
}

class DefaultQueryEngine(override val schema: Schema, private val strategies: List<QueryStrategy>) : QueryEngine  {
   override fun queryContext(factSet: Set<TypedInstance>): QueryContext {
      return QueryContext.from(schema, factSet, this)
   }

   override fun find(queryString: String, factSet: Set<TypedInstance>): QueryResult {
      val target = QueryParser(schema).parse(queryString)
      return find(target, factSet)
   }

   override fun find(target: QuerySpecTypeNode, factSet: Set<TypedInstance>): QueryResult {
      return find(setOf(target), factSet)
   }

   override fun find(target: Set<QuerySpecTypeNode>, factSet: Set<TypedInstance>): QueryResult {
      return find(target, queryContext(factSet))
   }

   override fun find(target: Set<QuerySpecTypeNode>, context: QueryContext): QueryResult {
      val matchedNodes = mutableMapOf<QuerySpecTypeNode, TypedInstance?>()
      // This is cheating, probably.
      // We only resolve top-level nodes, rather than traverse deeply.
      fun unresolvedNodes(): List<QuerySpecTypeNode> {
         return target.filterNot { matchedNodes.containsKey(it) }
      }

      val strategyIterator: Iterator<QueryStrategy> = strategies.iterator()
      while (strategyIterator.hasNext() && unresolvedNodes().isNotEmpty()) {
         val queryStrategy = strategyIterator.next()
         val strategyResult: QueryStrategyResult = queryStrategy.invoke(target, context)
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
         log().error("The following nodes weren't matched: ${unresolvedNodes().joinToString { ", " }}")
      }
//      TODO("Rebuild Path")
      return QueryResult(matchedNodes, unresolvedNodes().toSet(), path = null)
   }
}

