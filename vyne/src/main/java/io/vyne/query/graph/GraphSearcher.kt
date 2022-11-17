package io.vyne.query.graph

import com.google.common.base.Stopwatch
import es.usc.citius.hipster.model.impl.WeightedNode
import io.vyne.SchemaPathFindingGraph
import io.vyne.models.DataSource
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.query.InvocationConstraints
import io.vyne.query.PathExclusionCalculator
import io.vyne.query.SearchGraphExclusion
import io.vyne.query.excludedValues
import io.vyne.query.graph.SearchResult.Companion.noPath
import io.vyne.query.graph.edges.EvaluatableEdge
import io.vyne.query.graph.display.displayGraphJson
import io.vyne.query.graph.edges.EvaluatedEdge
import io.vyne.query.graph.edges.PathEvaluation
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Relationship
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Type
import io.vyne.utils.StrategyPerformanceProfiler
import mu.KotlinLogging
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

// This class is not optimized.  Need to investigate how to speed it up.
class GraphSearcher(
   private val startFact: Element,
   private val targetFact: Element,
   private val targetType: Type,
   private val graphBuilder: VyneGraphBuilder,
   private val invocationConstraints: InvocationConstraints
) {

   companion object {
      const val MAX_SEARCH_COUNT = 25
   }

   private enum class PathPrevaliationResult {
      EVALUATE,

      // We made some changes to the graph, so try again
      REQUERY,

      // Don't attempt this, but no point in requerying, as the graph is unchanged
      ABORT
   }

   val searchDescription: String by lazy {
      "Search ${this.startFact.label()} to ${this.targetFact.label()}"
   }

   private fun prevalidatePath(
      proposedPath: WeightedNode<Relationship, Element, Double>,
      excludedEdges: MutableList<EvaluatableEdge>
   ): PathPrevaliationResult {
      // TODO
      return PathPrevaliationResult.EVALUATE
   }

   suspend fun search(
       knownFacts: Collection<TypedInstance>,
       excludedServices: Set<SearchGraphExclusion<QualifiedName>>,
       excludedOperations: Set<SearchGraphExclusion<RemoteOperation>>,
       queryId: String,
       evaluator: PathEvaluator
   ): SearchResult {

      // TODO : EEEK!  We should be adding the instances, not the types.
      // This will cause problems when we have multiple facts of the same type,
      // as one may result in a happy path, and the other might not.
//      val factTypes = knownFacts.map { it.type }.toSet()

      val excludedOperationsNames = excludedOperations.map { it.excludedValue.qualifiedName }.toSet()
      val excludedInstance = mutableSetOf<TypedInstance>()
      val excludedEdges = mutableListOf<EvaluatableEdge>()
      val evaluatedPaths = EvaluatedPathSet()
      val trappedPaths = mutableSetOf<Int>()

      var searchCount = 0
      tailrec fun buildNextPath(): WeightedNode<Relationship, Element, Double>? {
         searchCount++
         if (searchCount > MAX_SEARCH_COUNT) {
            logger.error { "[$queryId] Search iterations exceeded max count. Stopping, lest we search forever in vein" }
            return null
         }
         logger.debug { "[$queryId] $searchDescription: Attempting to build search path $searchCount" }
         val facts = if (excludedInstance.isEmpty()) {
            knownFacts
         } else {
            knownFacts.filterNot { excludedInstance.contains(it) }
         }
         // Note: I think we can migrate to using exclusively excludedEdges (Not using excludedOperations
         // and excludedInstances)..as it should be a more powerful abstraction
         val proposedPath =
            findPath(
               facts,
               excludedOperationsNames,
               excludedEdges,
               excludedServices.excludedValues(),
               evaluatedPaths
            )

         return when {
            proposedPath == null -> null
            evaluatedPaths.containsPath(proposedPath) -> {
               /**
                * We used to bail out and terminate the search at this point.
                * However, HLP 88  reveals the fact that there might still be remaining valid solutions
                * which have not been discovered as the penalties imposed on the edges make these valid solutions
                * more expansive than remaining failed solutions, so we could hit this point without exploring these
                * valid solutions. Ideally this should never happen, however due to our current Graph construction logic
                * and the nature of A*, it might happen.
                */
               val pathHash = proposedPath.pathHashExcludingWeights()
               return if (trappedPaths.contains(pathHash)) {
                  if (logger.isDebugEnabled) {
                     logger.debug { "${proposedPath.pathDescription()} \n already been marked as duplicate, exiting search as we hit again whilst exploring" }
                     logger.debug { evaluatedPaths.printCurrentEdgeCosts() }
                  }
                  null
               } else {
                  logger.debug { "Found duplicate path: ${proposedPath.pathDescription()} \n" +
                     "  Increasing the cost of function args along it." }
                  /**
                   * We give the search another chance by excluding a certain edge (Relationship.IS_PARAMETER_ON edge for an operation
                   * along the duplicate path. If there is one operation in the path and it is the failed one, the edge chosen for the failed operation.
                   * If there are multiple operations in the path we chose the Relationship.IS_PARAMETER_ON edge for the first operation in the path.
                   */
                  val edgesToExclude = evaluatedPaths.getEvaluatedPath(proposedPath.pathHashExcludingWeights())?.let {
                     PathExclusionCalculator().findEdgesToExclude(it, invocationConstraints)
                  }
                  edgesToExclude?.let { excludedEdges.addAll(it) }
                  trappedPaths.add(pathHash)
                  buildNextPath()
               }
            }
            evaluatedPaths.containsEquivalentPath(proposedPath) -> {
               logger.debug {
                  val (simplifiedPath,equivalentPath) = evaluatedPaths.findEquivalentPath(proposedPath)
                  "[$queryId] Proposed path ${proposedPath.pathHashExcludingWeights()}: \n${proposedPath.pathDescription()} \nis equivalent to ${equivalentPath.pathHashExcludingWeights()} \n${equivalentPath.pathDescription()}.   \nBoth evaluate to: ${simplifiedPath.describePath()}"
               }
               // Even though we're not going to evaluate this path, we need to update the evaluatedPaths that this path has been ignored.
               // That will track the paths we would've walked, and tag them as penalized.  This affects weighting, which
               // allows another alternative path to be proposed.
               evaluatedPaths.addIgnoredPath(proposedPath)
               buildNextPath()
            }
            else -> {
               when (prevalidatePath(proposedPath, excludedEdges)) {
                  PathPrevaliationResult.EVALUATE -> proposedPath
                  PathPrevaliationResult.REQUERY -> buildNextPath()
                  PathPrevaliationResult.ABORT -> null
               }
            }
         }
      }

      var nextPath = buildNextPath()
      val failedAttempts = mutableListOf<DataSource>()
      while (nextPath != null) {

         val nextPathId = nextPath.pathHashExcludingWeights()
         evaluatedPaths.addProposedPath(nextPath)

         logger.debug { "[$queryId] $searchDescription - attempting path $nextPathId: \n${nextPath!!.pathDescription()}" }

         val evaluatedPath = evaluator(nextPath)
         evaluatedPaths.addEvaluatedPath(nextPathId, evaluatedPath)
         val (pathEvaluatedSuccessfully, resultValue, errorMessage) = wasSuccessful(evaluatedPath)
         val resultSatisfiesConstraints =
            pathEvaluatedSuccessfully && invocationConstraints.typedInstanceValidPredicate.isValid(resultValue)
         if (!pathEvaluatedSuccessfully) {
            logger.info { "$searchDescription - path $nextPathId failed - last error was $errorMessage" }
         }

         if (pathEvaluatedSuccessfully && resultSatisfiesConstraints) {
            logger.debug { "[$queryId] $searchDescription - path $nextPathId succeeded and returned instance of type ${resultValue?.typeName}" }
//            logger.trace { "[$queryId] $searchDescription - path $nextPathId succeeded with value $resultValue" }
            return SearchResult(resultValue, nextPath, failedAttempts)
         } else {
            if (pathEvaluatedSuccessfully && !resultSatisfiesConstraints) {
               logger.debug { "[$queryId] $searchDescription - path $nextPathId executed successfully, but result of $resultValue does not satisfy constraint defined by ${invocationConstraints.typedInstanceValidPredicate::class.simpleName}.  Will continue searching" }
            } else {
               logger.debug { "[$queryId] $searchDescription - path $nextPathId did not complete successfully, will continue searching" }
            }
         }

         // Collect the data sources of things we tried that didn't work out.
         resultValue?.source?.let { failedAttempts.add(it) }
         nextPath = buildNextPath()
      }
      // There were no search paths to evaluate.  Just exit
      //log().info("Failed to find path from ${startFact.label()} to ${targetFact.label()} after $searchCount searches")
      logger.debug { "[$queryId] $searchDescription ended - no more paths to evaluate" }
      return noPath(failedAttempts)
   }


   private fun wasSuccessful(evaluatedPath: List<PathEvaluation>): Triple<Boolean, TypedInstance?, String?> {
      val lastEdge = evaluatedPath.last()
      val success = lastEdge is EvaluatedEdge && lastEdge.wasSuccessful
      val resultValue = if (success) {
         StrategyPerformanceProfiler.profiled("Hipster.selectResultValue") {
            selectResultValue(evaluatedPath)
         }

      } else {
         // Even if the edge wasn't successful, operation invocations can return a TypedNull with details of their failure if an http operation failed.
         if (lastEdge.resultValue != null && lastEdge.resultValue is TypedNull) {
            lastEdge.resultValue
         } else null
      }
      val errorMessage = if (!success) {
         val evaluatedEdge = lastEdge as EvaluatedEdge
         evaluatedEdge.error
      } else {
         null
      }
      return Triple(success, resultValue, errorMessage)
   }


   private fun selectResultValue(evaluatedPath: List<PathEvaluation>): TypedInstance {
      // If the last node in the evaluated path is the type we're after, use that.
      val lastEdgeResult = evaluatedPath.last().resultValue
      if (lastEdgeResult != null && targetType.matches(lastEdgeResult.type)) {
         return lastEdgeResult
      }

//      if (lastEdgeResult != null && lastEdgeResult.type.isCalculated && targetType.matches(lastEdgeResult.type)) {
//         return lastEdgeResult
//      }

      // Handles the case where the target type is an alias for a collection type.
      if (lastEdgeResult != null &&
         targetType.isCollection &&
         targetType.isTypeAlias &&
         targetType.typeParameters.isNotEmpty() &&
         lastEdgeResult.type.matches(targetType.typeParameters.first())
      ) {
         return lastEdgeResult
      }

      // Note - in the old code, this called queryContext..getFactOrNull(targetType, FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT)
      // But, I want to understand why that's nessecary.
      // Investigate if we hit this point
      error("Unable to select result of ${targetType.longDisplayName} from graph search - expected the last edge result to match, but it didn't.")

   }

   private fun findPath(
      facts: Collection<TypedInstance>,
      excludedOperations: Set<QualifiedName>,
      excludedEdges: List<EvaluatableEdge>,
      excludedServices: Set<QualifiedName>,
      previouslyEvaluatedPaths: EvaluatedPathSet
   ): WeightedNode<Relationship, Element, Double>? {
      val graphBuildResult = graphBuilder.build(facts, excludedOperations, excludedEdges, excludedServices)
      logger.trace { """===================Query graph:========================
         | ${graphBuildResult.graph.displayGraphJson()}
         | ========================================================
      """.trimMargin() }
      return StrategyPerformanceProfiler.profiled("findPath") {
         val result = findPath(graphBuildResult.graph, previouslyEvaluatedPaths)
         result
      }

   }

   private fun findPath(
      graph: SchemaPathFindingGraph,
      evaluatedEdges: EvaluatedPathSet
   ): WeightedNode<Relationship, Element, Double>? {
      return graph.findPath(startFact, targetFact, evaluatedEdges)
   }

   private fun <R> logTimeTo(timeCollection: MutableList<Long>, operation: () -> R): R {
      val stopwatch = Stopwatch.createStarted()
      val result = operation()
      timeCollection.add(stopwatch.elapsed(TimeUnit.MILLISECONDS))
      return result
   }

}

private fun List<PathEvaluation>.lastEvaluatedEdge(): EvaluatedEdge? {
   return this.last() as? EvaluatedEdge
}
typealias PathEvaluator = suspend (WeightedNode<Relationship, Element, Double>) -> List<PathEvaluation>

data class SearchResult(val typedInstance: TypedInstance?, val path: WeightedNode<Relationship, Element, Double>?, val failedAttemptSources:List<DataSource>) {
   companion object {
      fun noPath(attemptedSources:List<DataSource>) = SearchResult(null, null, attemptedSources)
   }
}
