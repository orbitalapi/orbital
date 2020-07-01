package io.vyne.query.planner

import com.google.common.cache.CacheBuilder
import es.usc.citius.hipster.algorithm.Hipster
import es.usc.citius.hipster.graph.GraphSearchProblem
import es.usc.citius.hipster.model.impl.WeightedNode
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.query.FactDiscoveryStrategy
import io.vyne.query.QueryContext
import io.vyne.query.QuerySpecTypeNode
import io.vyne.query.QueryStrategy
import io.vyne.query.QueryStrategyResult
import io.vyne.query.graph.Element
import io.vyne.query.graph.ElementType
import io.vyne.query.graph.VyneGraphBuilder
import io.vyne.query.graph.instanceOfType
import io.vyne.query.graph.operationInvocation.OperationInvocationEvaluator
import io.vyne.schemas.Operation
import io.vyne.schemas.OperationNames
import io.vyne.schemas.Relationship
import io.vyne.schemas.Service
import io.vyne.schemas.Type
import io.vyne.utils.timed
import java.util.concurrent.TimeUnit

class ProjectionHeuristicsQueryStrategy(private val operationInvocationEvaluator: OperationInvocationEvaluator) : QueryStrategy {
   private val cache = CacheBuilder.newBuilder()
      .maximumSize(10) // arbitary cache size, we can explore tuning this later
      .weakKeys()
      .build<Type, ProjectionHeuristicsGraphSearchResult>()
   override fun invoke(target: Set<QuerySpecTypeNode>, context: QueryContext): QueryStrategyResult {
      if (!context.isProjecting) {
         return QueryStrategyResult.empty()
      }
      val targetType = target.first().type
      val searchResult =  cache.get(targetType) {
         fetchFromGraph(target, context)
      }

      val queryStrategyResult = timed(name = "heuristics result", timeUnit = TimeUnit.MICROSECONDS, log = false) {
         searchResult.pair?.first?.let {
            cache.put(targetType, searchResult)
            val joinedFact = FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT.getFact(context, searchResult.pair.second)
            val matchedInstance = it[joinedFact]
            return@timed queryStrategyResult(matchedInstance, context, targetType, target)
         }
      }
      return queryStrategyResult ?: QueryStrategyResult.empty()
   }

   private fun queryStrategyResult(matchedInstance: MutableList<TypedInstance>?, context: QueryContext, targetType: Type, target: Set<QuerySpecTypeNode>): QueryStrategyResult? {
      matchedInstance?.let { joinMatch ->
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
               // Add 'Trade' into the context, so subsequent target projection props might be resolved..
               context.addFact(match)
               return QueryStrategyResult(mapOf(target.first() to retVal))
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
            matchedInstance.removeAt(0)
            return QueryStrategyResult(mapOf(target.first() to  TypedCollection.from(retVal)))
         }
      }
      return null
   }

   /**
    * Try to find an operation that takes Attribute_of_SourceType[] -> Type_That_Provides_Missing_Attribute[]
    * Example: We're looking for a tradeNo, our source type is an Order, with an OrderId attribute.
    * tradeNo is an attribute of Trade, and this tries to find the following operation
    * operation(orderIds: OrderId[]): Trade[]
    */
   private fun fetchFromGraph(target: Set<QuerySpecTypeNode>, context: QueryContext): ProjectionHeuristicsGraphSearchResult {
      return timed("fetch from Graph") {
         graphSearchResult(target, context)?.let { firstMatch ->
            findFetchManyOperation(firstMatch, context)?.let { (candidateService, candidateOperation, joinType) ->
               return@timed processRemoteCallResults(candidateOperation, candidateService, context, joinType)
            }
         }
         ProjectionHeuristicsGraphSearchResult(null)
      }
   }

   private fun graphSearchResult(target: Set<QuerySpecTypeNode>, context: QueryContext): WeightedNode<Relationship, Element, Double>? {
      val graph = VyneGraphBuilder(context.schema).build(context.facts.map { it.type }.toSet())
      return context.facts.asSequence()
         .mapNotNull { fact ->
            val searchStart = instanceOfType(fact.type)
            val targetElement = Element(target.first().type.fullyQualifiedName, ElementType.TYPE)
            val problem = GraphSearchProblem.startingFrom(searchStart).`in`(graph).takeCostsFromEdges().build()
            val result = Hipster.createAStar(problem).search(targetElement).goalNode
            if (result.state() != targetElement) null else result
         }.firstOrNull()
   }

   private fun findFetchManyOperation(firstMatch: WeightedNode<Relationship, Element, Double>?, context: QueryContext):  Triple<Service, Operation, Type>? {
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

   private fun processRemoteCallResults(candidateOperation: Operation, candidateService: Service, context: QueryContext, joinType: Type): ProjectionHeuristicsGraphSearchResult {
      val firstOperationArgumentType = candidateOperation.parameters.first().type
      context.parent?.let { qc ->
         val requiredArgument = FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY_UNWRAP_COLLECTION.getFact(qc, firstOperationArgumentType)
         requiredArgument?.let { arg ->
            val result = operationInvocationEvaluator.invocationService.invokeOperation(candidateService, candidateOperation, setOf(arg), qc)
            val arguments = arg as TypedCollection
            val firstMatch = (result as TypedCollection).firstOrNull { item ->  (item as TypedObject).values.any { value -> value.valueEquals(arguments.first()) }}
            val key = (firstMatch as TypedObject?)?.value?.entries?.first { typedObjectValue -> typedObjectValue.value.valueEquals(arguments.first()) }?.key
            val resultMap =  arguments
               .map { argument -> argument to result.filter { item ->  key?.let { attr -> (item as TypedObject).value[attr]?.valueEquals(argument) } == true}.toMutableList() }
               .toMap()
            return ProjectionHeuristicsGraphSearchResult(Pair(resultMap, joinType))
         }
      }
      return ProjectionHeuristicsGraphSearchResult(null)
   }
}

data class ProjectionHeuristicsGraphSearchResult(val pair: Pair<Map<TypedInstance, MutableList<TypedInstance>>, Type>?)
