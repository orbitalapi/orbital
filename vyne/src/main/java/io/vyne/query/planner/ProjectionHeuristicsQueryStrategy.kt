package io.vyne.query.planner

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import es.usc.citius.hipster.algorithm.Hipster
import es.usc.citius.hipster.graph.GraphSearchProblem
import es.usc.citius.hipster.model.impl.WeightedNode
import io.vyne.VyneGraphBuilderCacheSettings
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.query.*
import io.vyne.query.graph.Element
import io.vyne.query.graph.ElementType
import io.vyne.query.graph.VyneGraphBuilder
import io.vyne.query.graph.instanceOfType
import io.vyne.query.graph.operationInvocation.OperationInvocationEvaluator
import io.vyne.schemas.Operation
import io.vyne.schemas.OperationNames
import io.vyne.schemas.Relationship
import io.vyne.schemas.Schema
import io.vyne.schemas.Service
import io.vyne.schemas.Type
import io.vyne.utils.log
import io.vyne.utils.timed
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.TimeUnit

class ProjectionHeuristicsQueryStrategy(private val operationInvocationEvaluator: OperationInvocationEvaluator,
                                        private val vyneGraphBuilderCacheSettings: VyneGraphBuilderCacheSettings) : QueryStrategy {
   private val cache = CacheBuilder.newBuilder()
      .weakKeys()
      .build<Type, ProjectionHeuristicsGraphSearchResult>()

   private val schemaGraphCache = CacheBuilder.newBuilder()
      .maximumSize(5) // arbitary cache size, we can explore tuning this later
      .weakKeys()
      .build(object : CacheLoader<Schema, VyneGraphBuilder>() {
         override fun load(schema: Schema): VyneGraphBuilder {
            return VyneGraphBuilder(schema, vyneGraphBuilderCacheSettings)
         }

      })

   override suspend fun invoke(target: Set<QuerySpecTypeNode>, context: QueryContext, invocationConstraints: InvocationConstraints): QueryStrategyResult {
      if (!context.isProjecting) {
         return QueryStrategyResult.empty()
      }
      val spec = invocationConstraints.typedInstanceValidPredicate
      val targetType = target.first().type
      val searchResult: ProjectionHeuristicsGraphSearchResult = cache.get(targetType) {
         fetchFromGraph(target, context, spec)
      }

      if (searchResult.isEmpty || searchResult == ProjectionHeuristicsGraphSearchResult.UNABLE_TO_SEARCH) {
         return QueryStrategyResult.empty()
      }

      val queryStrategyResult = timed(name = "heuristics result", timeUnit = TimeUnit.MICROSECONDS, log = false) {
//            Commmenting out this line, as I don't think it makes snese.
         // Isn't thie the value we've just fetched from the cache?  Why add it back?
//            cache.put(targetType, searchResult)

         FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT.getFact(context, searchResult.joinType!!, spec = AlwaysGoodSpec)
            ?.let { joinedFact ->
               searchResult.queryResultByKey[joinedFact]
            }?.let { matchedInstance ->
               queryStrategyResult(matchedInstance, context, targetType, target, spec)
            } ?: QueryStrategyResult.empty()
      }
      return queryStrategyResult
   }

   private fun queryStrategyResult(joinMatch: ProjectionResultList, context: QueryContext, targetType: Type, target: Set<QuerySpecTypeNode>, spec: TypedInstanceValidPredicate): QueryStrategyResult? {
         if (joinMatch.size == 0) return null
         if (joinMatch.size == 1) {
            // one to one mapping case.
            // Example: Whilst discovering 'TradeNo', we've invoked:
            // operation(OrderIds: OrderId[]): Trade[]
            // and for orderId = '1' operation returns only one Trade instance.
            joinMatch.first().let { match ->
               val typedObject = match as TypedObject
               // Example:
               val retVal = typedObject.values.first { typedValue -> typedValue.type.isAssignableTo(targetType, true) }
               return if (spec.isValid(retVal)) {
                  // Add 'Trade' into the context, so subsequent target projection props might be resolved..
                  context.addFact(match)

                  QueryStrategyResult( flow { emit(retVal) } )
               } else {
                  QueryStrategyResult.empty()
               }
            }
         } else {
            // one to many case.
            // Example: Whilst discovering 'TradeNo', we've invoked:
            // operation(OrderIds: OrderId[]): Trade[]
            // and for orderId = '1' operation returns only 3 Trades instance.
            // joinMatch now contains now 3 Trade instances for the given OrderId.
            val retVal = joinMatch.map { match ->
               val typObj = match as TypedObject
               // Extract the target property from Trade object (e.g. extract TradeNo from Trade)
               typObj.values.first { typedValue -> typedValue.type.isAssignableTo(targetType, true) }
            }
            // Add Trade object into the context, so that subsequent projection props might be extracted from Trade.
            context.addFact(joinMatch.first())
            // Remove the Trade just added into the context, as we'll call here again through ObjectBuilder for remaining matched Trades.
            joinMatch.removeFirst()
            return QueryStrategyResult( TypedCollection.from(retVal).value.asFlow() )
      }
   }

   /**
    * Try to find an operation that takes Attribute_of_SourceType[] -> Type_That_Provides_Missing_Attribute[]
    * Example: We're looking for a tradeNo, our source type is an Order, with an OrderId attribute.
    * tradeNo is an attribute of Trade, and this tries to find the following operation
    * operation(orderIds: OrderId[]): Trade[]
    */
   private fun fetchFromGraph(target: Set<QuerySpecTypeNode>, context: QueryContext, spec: TypedInstanceValidPredicate): ProjectionHeuristicsGraphSearchResult {
      return timed("fetch from Graph", log = false) {
         graphSearchResult(target, context)?.let { firstMatch ->
            findFetchManyOperation(firstMatch, context)?.let { (candidateService, candidateOperation, joinType) ->
               // TODO ACOWAN return@timed processRemoteCallResults(candidateOperation, candidateService, context, joinType, spec)
            }
         }
         // else
         ProjectionHeuristicsGraphSearchResult.UNABLE_TO_SEARCH
      }
   }

   private fun graphSearchResult(target: Set<QuerySpecTypeNode>, context: QueryContext): WeightedNode<Relationship, Element, Double>? {
      val graphBuilder = schemaGraphCache.get(context.schema)
      val graph = graphBuilder.build(types = context.facts.map { it.type }.toSet(), excludedServices = context.excludedServices.toSet())
      return context.facts.asSequence()
         .mapNotNull { fact ->
            val searchStart = instanceOfType(fact.type)
            val targetElement = Element(target.first().type.fullyQualifiedName, ElementType.TYPE)
            val problem = GraphSearchProblem.startingFrom(searchStart).`in`(graph).takeCostsFromEdges().build()
            val result = Hipster.createAStar(problem).search(targetElement).goalNode
            if (result.state() != targetElement) null else result
         }.firstOrNull()
   }

   private fun findFetchManyOperation(firstMatch: WeightedNode<Relationship, Element, Double>?, context: QueryContext): Triple<Service, Operation, Type>? {
      firstMatch?.let { it ->
         it.path().firstOrNull { path -> path.action() == Relationship.PROVIDES }?.let { node ->
            val operationElement = node.previousNode().state()
            val (serviceName, operationName) = OperationNames.serviceAndOperation(operationElement.valueAsQualifiedName())
            val service = context.schema.service(serviceName)
            val operation = service.operation(operationName)
            val returnType = operation.returnType
            if (!returnType.isCollection && operation.parameters.size == 1 && !operation.parameters.first().type.isCollection) {
               val candidateOperations =
                  context.schema
                     .operationsWithReturnTypeAndWithSingleArgument(returnType.asArrayType(), operation.parameters.first().type.asArrayType())
               return if (candidateOperations.size == 1) {
                  Triple(candidateOperations.first().first, candidateOperations.first().second, operation.parameters.first().type)
               } else {
                  null
               }
            }
         }
      }
      return null
   }

   private suspend fun processRemoteCallResults(candidateOperation: Operation, candidateService: Service, context: QueryContext, joinType: Type, spec: TypedInstanceValidPredicate): ProjectionHeuristicsGraphSearchResult {
      val firstOperationArgumentType = candidateOperation.parameters.first().type
      context.parent?.let { parentQueryContext ->
         // Using AlwaysGood build spec because at the time of writing we're not passing specs this deep.
         // Let's revisit as / when needed
         val requiredArgument = FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY_UNWRAP_COLLECTION.getFact(parentQueryContext, firstOperationArgumentType, spec = AlwaysGoodSpec)
            ?: return ProjectionHeuristicsGraphSearchResult(emptyMap(), joinType)

         val result = operationInvocationEvaluator.invocationService.invokeOperation(candidateService, candidateOperation, setOf(requiredArgument), parentQueryContext) as TypedCollection
         if (result.isEmpty()) {
            // bail early if we received nothing
            return ProjectionHeuristicsGraphSearchResult(emptyMap(), joinType)
         }
         val arguments = requiredArgument as TypedCollection

         // Match the results against the arguments that were collected.
         // For now, assume all the args are a single type
         val argumentTypes = arguments.map { it.type }.distinct()
         if (argumentTypes.size > 1) {
            log().warn("Multiple argument types are not currently supported - some results may not match, resulting in further HTTP operations downstream")
         }
         val argType = argumentTypes.first()
         val firstResultEntry = result.first() as TypedObject
         val keyFieldName = firstResultEntry.entries
            .firstOrNull { (_, instance) -> instance.type.name == argType.name }
            ?.key
         if (keyFieldName == null) {
            log().warn("The result of the batch operation ${candidateOperation.qualifiedName} did not contain an entry with type ${argType.name} meaning results cannot be grouped.  Aborting the batch lookup")
            return ProjectionHeuristicsGraphSearchResult.empty(joinType)
         }
         val resultsByKey = result.groupBy { resultEntry ->
            val resultEntryObject = resultEntry as TypedObject
            resultEntryObject[keyFieldName]
         }.mapValues { (_, value) ->
            // Only include results which pass the TypedInstance spec we were provided
            value.filter { spec.isValid(it) }
         }.filter { (_, values) -> values.isNotEmpty() }
            .mapValues { (_, values) -> values.toProjectionResultList() }

         return ProjectionHeuristicsGraphSearchResult(resultsByKey, joinType)
      }
      return ProjectionHeuristicsGraphSearchResult.empty(joinType)
   }
}

// TODO : Fix the nullable on type
data class ProjectionHeuristicsGraphSearchResult(val queryResultByKey: Map<TypedInstance, ProjectionResultList>, val joinType: Type?) {
   companion object {
      fun empty(type:Type) = ProjectionHeuristicsGraphSearchResult(emptyMap(), type)
      val UNABLE_TO_SEARCH = ProjectionHeuristicsGraphSearchResult(emptyMap(), null)
   }
   val isEmpty = queryResultByKey.isEmpty()
}
