package io.osmosis.polymer.query

import io.osmosis.polymer.SchemaPathResolver
import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.schemas.Schema
import io.osmosis.polymer.utils.log

interface QueryEngine : SchemaPathResolver {

   val schema: Schema
   fun find(target: QuerySpecTypeNode, factSet: Set<TypedInstance> = emptySet()): QueryResult
   fun find(target: Set<QuerySpecTypeNode>, factSet: Set<TypedInstance> = emptySet()): QueryResult
   fun find(target: Set<QuerySpecTypeNode>, context: QueryContext): QueryResult
   fun queryContext(factSet: Set<TypedInstance> = emptySet()): QueryContext
}

/**
 * An extension to the QueryEngine, allowing for the provision of initial state
 */
class StatefulQueryEngine(private val initialState: Set<TypedInstance>, private val queryEngine: QueryEngine) : QueryEngine by queryEngine {
   fun find(queryString: String): QueryResult {
      return find(QueryParser(queryEngine.schema).parse(queryString), initialState)
   }

   fun queryContext(): QueryContext {
      return queryEngine.queryContext(initialState)
   }
}

class DefaultQueryEngine(override val schema: Schema, private val strategies: List<QueryStrategy>, private val schemaPathResolver: SchemaPathResolver) : QueryEngine, SchemaPathResolver by schemaPathResolver {
   override fun queryContext(factSet: Set<TypedInstance>): QueryContext {
      return QueryContext.from(schema, factSet, this)
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

      val iterator = strategies.iterator()
      while (iterator.hasNext() && unresolvedNodes().isNotEmpty()) {
         val strategyResult: QueryStrategyResult = iterator.next().invoke(target, context)
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
      return QueryResult(matchedNodes, unresolvedNodes().toSet())
   }
}

