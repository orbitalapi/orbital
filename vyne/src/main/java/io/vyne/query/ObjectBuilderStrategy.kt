package io.vyne.query

import io.vyne.models.TypedNull
import io.vyne.models.format.ModelFormatSpec
import kotlinx.coroutines.flow.asFlow

/**
 * Purpose of this strategy is build target type field by field basis where no other QueryStrategy finds the target
 * through service invocations or through context fact lookup.
 *
 * As an example, consider this simple taxonomy:
 *
 * type Name inherits String
 * model Person {
 *    name: Name
 * }
 *
 * service NameService {
 *    operation getName(): Name
 * }
 *
 * given:
 * find { Person }
 *
 * In the above scenario,
 * 1. we don't have an existing Person fact, so Vyne can't produce the result through context fact lookup
 * 2. There is no operation that returns a Person
 *
 * But what we have is that a single operation returning 'Name', so Vyne can invoke the service to fetch it and then construct the Person with the supplied Name
 * which is possible through the ObjectBuilder, so the summarised discovery path would be:
 *
 * find Person ->  ObjectBuilderStrategy -> find Name -> HipsterDiscoverGraphQueryStrategy -> NameService::getName()
 *
 * However, there is a catch, consider the below taxonomy which is the same as above except it is without NameService:
 *
 * type Name inherits String
 * model Person {
 *    name: Name
 * }
 *
 * same query:
 *
 * find { Person }
 *
 * would yield an infinite loop as this QueryStrategy would invoke ObjectBuilder and ObjectBuilder re-invokes 'queryEngine.find' which in turn will re-invoke this strategy
 * eventually ending up with an infinite loop. we use QueryStrategyValidPredicate to break this cycle and filter out this strategy when 'ObjectBuilder' invokes queryEngine.doFind
 */
class ObjectBuilderStrategy(val formatSpecs:List<ModelFormatSpec> = emptyList()): QueryStrategy {
   override suspend fun invoke(target: Set<QuerySpecTypeNode>, context: QueryContext, invocationConstraints: InvocationConstraints): QueryStrategyResult {
      val match = ObjectBuilder(context.queryEngine, context, target.first().type, functionRegistry = context.schema.functionRegistry, formatSpecs = formatSpecs).build()
      return  when {
         match != null && match is TypedNull ->  QueryStrategyResult.searchFailed()
         match != null -> QueryStrategyResult( listOf(match).asFlow() )
         else -> QueryStrategyResult.searchFailed()
      }
   }
}
