package io.vyne.query

import io.vyne.query.graph.ElementType
import io.vyne.query.graph.edges.EvaluatedEdge
import io.vyne.query.graph.edges.PathEvaluation
import io.vyne.schemas.Relationship

/**
 * The purpose os this interface is to encapsulate the Edge penalty cost calculation from @see io.vyne.query.EvaluatedPathSet
 * so that we can try different cost calculation strategies by implementing this interface.
 */
interface SearchPenaltyProvider {
   /**
    * When a path is failed return the list of edge penalties for it.
    */
   fun penaliseEdgesForFailedEvaluation(evaluatedPath: List<PathEvaluation>): List<PenalizedEdge>

   /**
    * This is not being used at the moment but added in case a different list of edge penalties can be calculated
    * when the failed path is already an evaluated (i.e. duplicate) path.
    */
   fun increaseFunctionCallArgsCostsAlongThePath(evaluatedPath: List<PathEvaluation>): List<PenalizedEdge>

}

abstract class BaseSearchPenaltyProviderImpl : SearchPenaltyProvider {

   protected fun indexOfProvidedInstanceNumber(evaluatedPath: List<PathEvaluation>, indexOfFailedOperation: Int): Int? {
      return (indexOfFailedOperation downTo 0)
         .firstOrNull { index ->
            val edge = evaluatedPath[index]
            edge is EvaluatedEdge &&
               edge.edge.vertex1.elementType == ElementType.PROVIDED_INSTANCE_MEMBER
         }
   }

   protected fun indexOfIsParameterOn(evaluatedPath: List<PathEvaluation>, indexOfFailedOperation: Int): Int? {
      return (indexOfFailedOperation downTo 0)
         .firstOrNull { index ->
            val edge = evaluatedPath[index]
            edge is EvaluatedEdge &&
               edge.edge.vertex1.elementType == ElementType.PARAMETER
         }
   }

   protected fun relationshipPenalty(
      evaluatedPath: List<PathEvaluation>,
      indexOfParameterInput: Int?,
      relationship: Relationship,
      penalty: Double): PenalizedEdge? {
      return if (indexOfParameterInput != null && evaluatedPath.size > (indexOfParameterInput)) {
         val edge = evaluatedPath[indexOfParameterInput]
         if (edge is EvaluatedEdge &&
            edge.edge.relationship == relationship) {
            PenalizedEdge(
               edge,
               evaluatedPath,
               "Input into a failed operation",
               penalty = penalty
            )
         } else null
      } else null
   }

   companion object {
      const val PenaltyCost = 100.0
      /**
       * This looks for a failed operation call, then backtracks to find the provider
       * of the inputs.
       *
       * We penalize the input, as well as the operation, so that the graph will try to find
       * another way to supply different inputs to the operation
       */
      fun findProvidedInstanceMemberVertexForFailedOperation(evaluatedPath: List<PathEvaluation>): EvaluatedEdge? {
         val indexOfFailedOperation = evaluatedPath.indexOfFirst {
            it is EvaluatedEdge &&
               !it.wasSuccessful &&
               it.edge.vertex1.elementType == ElementType.OPERATION
         }
         if (indexOfFailedOperation == -1) return null

         // This finds the PROVIDED_INSTANCE_MEMBER edge which was used to populate the input
         // parameter onto the operation.
         val indexOfParameterInput = (indexOfFailedOperation downTo 0)
            .firstOrNull { index ->
               val edge = evaluatedPath[index]
               edge is EvaluatedEdge &&
                  edge.edge.vertex1.elementType == ElementType.PROVIDED_INSTANCE_MEMBER
            }
         return if (indexOfParameterInput != null) {
            evaluatedPath[indexOfParameterInput] as EvaluatedEdge
         } else null
      }

      fun findFirstIsParameterOnEdgeForFailedOperation(evaluatedPath: List<PathEvaluation>): EvaluatedEdge? {
         val operationIndexes = mutableMapOf<Int, Boolean>()
            evaluatedPath.forEachIndexed { index, pathEvaluation ->
              if(pathEvaluation is EvaluatedEdge &&
               pathEvaluation.edge.vertex1.elementType == ElementType.OPERATION) {
                 operationIndexes[index] = pathEvaluation.wasSuccessful
              }
         }

         if (operationIndexes.isEmpty()) {
            return null
         }

         val indexOfFailedOperation = when {
             operationIndexes.size > 1 -> operationIndexes.keys.minOrNull()!!
             else -> operationIndexes.filter { entry -> !entry.value }.map { it.key }.firstOrNull()

         } ?: if (operationIndexes.size == 1) operationIndexes.keys.first() else null



         // This finds the PROVIDED_INSTANCE_MEMBER edge which was used to populate the input
         // parameter onto the operation.
         val indexOfParameterInput = indexOfFailedOperation?.downTo(0)
            ?.firstOrNull { index ->
               val edge = evaluatedPath[index]
               edge is EvaluatedEdge &&
                  edge.edge.vertex1.elementType == ElementType.PARAMETER &&
                  edge.edge.relationship == Relationship.IS_PARAMETER_ON
            }
         return if (indexOfParameterInput != null) {
            evaluatedPath[indexOfParameterInput] as EvaluatedEdge
         } else null
      }
   }
}

class PenaliseOperationAndProvidedInstanceMember : BaseSearchPenaltyProviderImpl() {

   /**
    * This looks for a failed operation call, then backtracks to find the provider
    * of the inputs.
    *
    * We penalize the input, as well as the operation, so that the graph will try to find
    * another way to supply different inputs to the operation
    */
   private fun findPenaltiesForFailedOperation(evaluatedPath: List<PathEvaluation>, transitionCost:Double): List<PenalizedEdge> {
      val indexOfFailedOperation = evaluatedPath.indexOfFirst {
         it is EvaluatedEdge &&
            !it.wasSuccessful &&
            it.edge.vertex1.elementType == ElementType.OPERATION
      }
      if (indexOfFailedOperation == -1) return emptyList()
      val failedOperationEdge = evaluatedPath[indexOfFailedOperation] as EvaluatedEdge
      val failedOperationPenalty = PenalizedEdge(
         failedOperationEdge,
         evaluatedPath,
         "Operation failed with error ${failedOperationEdge.error}",
         penalty = transitionCost
      )
      // This finds the PROVIDED_INSTANCE_MEMBER edge which was used to populate the input
      // parameter onto the operation.
      val indexOfParameterInput = (indexOfFailedOperation downTo 0)
         .firstOrNull { index ->
            val edge = evaluatedPath[index]
            edge is EvaluatedEdge &&
               edge.edge.vertex1.elementType == ElementType.PROVIDED_INSTANCE_MEMBER
         }
      val failedInputPenalty = if (indexOfParameterInput != null) {
         PenalizedEdge(
            evaluatedPath[indexOfParameterInput] as EvaluatedEdge,
            evaluatedPath,
            "Input into a failed operation",
            penalty = transitionCost
         )
      } else null
      return listOfNotNull(
         failedOperationPenalty,
         failedInputPenalty
      )
   }

   override fun increaseFunctionCallArgsCostsAlongThePath(evaluatedPath: List<PathEvaluation>): List<PenalizedEdge>{
      val penalisedEdges = findPenaltiesForFailedOperation(evaluatedPath, 2 * PenaltyCost)
      return penalisedEdges
   }

   override fun penaliseEdgesForFailedEvaluation(evaluatedPath: List<PathEvaluation>): List<PenalizedEdge> {
      val penalisedEdges = findPenaltiesForFailedOperation(evaluatedPath,  PenaltyCost)
      return penalisedEdges
   }
}


