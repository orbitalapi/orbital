package io.vyne.query.planner

import com.google.common.cache.CacheBuilder
import es.usc.citius.hipster.algorithm.Hipster
import es.usc.citius.hipster.graph.GraphSearchProblem
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
import io.vyne.schemas.OperationNames
import io.vyne.schemas.Relationship
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
            matchedInstance?.let { match ->
               val typedObject = match as TypedObject
               val retVal = typedObject.values.first { typedValue -> typedValue.type.isAssignableTo(targetType, true) }
               context.addFact(match)
               QueryStrategyResult(mapOf(target.first() to retVal))
            }
         }
      }

        return queryStrategyResult ?: QueryStrategyResult.empty()

   }
   private fun fetchFromGraph(target: Set<QuerySpecTypeNode>, context: QueryContext): ProjectionHeuristicsGraphSearchResult {
      return timed("fetch from Graph") {
         val graph = VyneGraphBuilder(context.schema).build(context.facts.map { it.type }.toSet())
         val firstMatch = timed("projection heuristics graph match") {
            context.facts.asSequence()
               .mapNotNull { fact ->
                  val searchStart = instanceOfType(fact.type)
                  val targetElement = Element(target.first().type.fullyQualifiedName, ElementType.TYPE)
                  val problem = GraphSearchProblem.startingFrom(searchStart).`in`(graph).takeCostsFromEdges().build()
                  val result = Hipster.createAStar(problem).search(targetElement).goalNode
                  return@mapNotNull if (result.state() != targetElement) null else result
               }.firstOrNull()
         }

         firstMatch?.let { it ->
            it.path().firstOrNull { path -> path.action() == Relationship.PROVIDES }?.let { node ->
               val operationElement = node.previousNode().state()
               val (serviceName, operationName) = OperationNames.serviceAndOperation(operationElement.valueAsQualifiedName())
               val service = context.schema.service(serviceName)
               val operation = service.operation(operationName)
               val returnType = operation.returnType
               if (!returnType.isCollection && operation.parameters.size == 1 && !operation.parameters.first().type.isCollection) {
                  val firstOperationArgumentType = operation.parameters.first().type
                  val argumentType = firstOperationArgumentType.asArrayType()
                  val candidateOperations =
                     context.schema
                        .operationsWithReturnTypeAndWithSingleArgument(returnType.asArrayType(), operation.parameters.first().type.asArrayType())
                  if (candidateOperations.size == 1) {
                     val (candidateService, candidateOperation) = candidateOperations.first()
                     context.parent?.let { qc ->
                        val requiredArgument = FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY_UNWRAP_COLLECTION.getFact(qc, argumentType)
                        requiredArgument?.let { arg ->
                           val result = operationInvocationEvaluator.invocationService.invokeOperation(candidateService, candidateOperation, setOf(arg), qc)
                           val arguments = arg as TypedCollection
                           val l = (result as TypedCollection).firstOrNull { item ->  (item as TypedObject).values.any { value -> value.valueEquals(arguments.first()) }}
                           val key = (l as TypedObject?)?.value?.entries?.first { typedObjectValue -> typedObjectValue.value.valueEquals(arguments.first()) }?.key
                           val resultMap =  arguments
                               .map { argument -> argument to result.firstOrNull { item ->  key?.let { attr -> (item as TypedObject).value[attr]?.valueEquals(argument) } == true} }
                              .toMap()
                           return@timed ProjectionHeuristicsGraphSearchResult(Pair(resultMap, firstOperationArgumentType))
                        }
                     }
                  }
               }
            }
         }
         ProjectionHeuristicsGraphSearchResult(null)
      }
   }
}

data class ProjectionHeuristicsGraphSearchResult(val pair: Pair<Map<TypedInstance, TypedInstance?>, Type>?)
