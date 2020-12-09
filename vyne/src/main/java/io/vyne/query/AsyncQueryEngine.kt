package io.vyne.query

import io.vyne.FactSetId
import io.vyne.FactSets
import io.vyne.models.TypedInstance
import io.vyne.schemas.Operation
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class AsyncQueryEngine(private val queryEngine: QueryEngine, private val executor: ExecutorService = DEFAULT_EXECUTOR) {
   companion object {
      val DEFAULT_EXECUTOR = Executors.newFixedThreadPool(10)
   }

   val schema: Schema = queryEngine.schema
   fun find(type: Type, context: QueryContext, spec: TypedInstanceValidPredicate = AlwaysGoodSpec): Future<QueryResult> {
      return executor.submit<QueryResult> {
         queryEngine.find(type, context, spec)
      }
   }

   fun find(queryString: QueryExpression, context: QueryContext, spec: TypedInstanceValidPredicate = AlwaysGoodSpec): Future<QueryResult> {
      return executor.submit<QueryResult> {
         queryEngine.find(queryString, context, spec)
      }
   }

   fun find(target: QuerySpecTypeNode, context: QueryContext, spec: TypedInstanceValidPredicate = AlwaysGoodSpec): Future<QueryResult> {
      return executor.submit<QueryResult> {
         queryEngine.find(target, context, spec)
      }
   }

   fun find(target: Set<QuerySpecTypeNode>, context: QueryContext, spec: TypedInstanceValidPredicate = AlwaysGoodSpec): Future<QueryResult> {
      return executor.submit<QueryResult> {
         queryEngine.find(target, context, spec)
      }
   }

   fun find(target: QuerySpecTypeNode, context: QueryContext, excludedOperations: Set<Operation>, spec: TypedInstanceValidPredicate = AlwaysGoodSpec): Future<QueryResult> {
      return executor.submit<QueryResult> {
         queryEngine.find(target, context, excludedOperations, spec)
      }
   }

   fun findAll(queryString: QueryExpression, context: QueryContext): Future<QueryResult> {
      return executor.submit<QueryResult> {
         queryEngine.findAll(queryString, context)
      }
   }

   fun queryContext(
      factSetIds: Set<FactSetId> = setOf(FactSets.DEFAULT),
      additionalFacts: Set<TypedInstance> = emptySet()): QueryContext = queryEngine.queryContext(factSetIds, additionalFacts)

   fun build(type: Type, context: QueryContext): Future<QueryResult> = build(TypeNameQueryExpression(type.fullyQualifiedName), context)
   fun build(query: QueryExpression, context: QueryContext): Future<QueryResult> {
      return executor.submit<QueryResult> {
         queryEngine.build(query, context)
      }
   }

   fun parse(queryExpression: QueryExpression): Set<QuerySpecTypeNode> = queryEngine.parse(queryExpression)
}
