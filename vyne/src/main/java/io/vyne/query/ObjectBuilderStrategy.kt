package io.vyne.query

import io.vyne.models.TypedInstance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

class ObjectBuilderStrategy: QueryStrategy {
   override suspend fun invoke(target: Set<QuerySpecTypeNode>, context: QueryContext, invocationConstraints: InvocationConstraints): QueryStrategyResult {
      if (context.isProjecting) {
         /**
          * During the projection ObjectBuilder has already been invoked, so don't lead to infinite loops.
          */
         return QueryStrategyResult.searchFailed()
      }
      return QueryStrategyResult.searchFailed()
      val match = ObjectBuilder(context.queryEngine, context, target.first().type, functionRegistry = context.schema.functionRegistry, allowRecursion = false).build()
      return if (match != null) {
         QueryStrategyResult( listOf(match).asFlow() as Flow<TypedInstance>)
      } else {
         return QueryStrategyResult.searchFailed()
      }
   }
}
