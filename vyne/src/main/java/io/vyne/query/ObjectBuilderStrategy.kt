package io.vyne.query

import io.vyne.models.TypedInstance
import io.vyne.models.format.ModelFormatSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

class ObjectBuilderStrategy(val formatSpecs:List<ModelFormatSpec> = emptyList()): QueryStrategy {
   override suspend fun invoke(target: Set<QuerySpecTypeNode>, context: QueryContext, invocationConstraints: InvocationConstraints): QueryStrategyResult {
      if (context.isProjecting) {
         /**
          * During the projection ObjectBuilder has already been invoked, so don't lead to infinite loops.
          */
         return QueryStrategyResult.searchFailed()
      }
      val match = ObjectBuilder(context.queryEngine, context, target.first().type, functionRegistry = context.schema.functionRegistry, allowRecursion = false, formatSpecs = formatSpecs).build()
      return if (match != null) {
         QueryStrategyResult( listOf(match).asFlow() as Flow<TypedInstance>)
      } else {
         return QueryStrategyResult.searchFailed()
      }
   }
}
