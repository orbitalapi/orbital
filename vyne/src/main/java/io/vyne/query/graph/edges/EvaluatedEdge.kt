package io.vyne.query.graph.edges

import io.vyne.models.TypedInstance
import io.vyne.query.graph.Element

data class EvaluatedEdge(
    val edge: EvaluatableEdge,
    val resultGraphElement: Element?,
    override val resultValue: TypedInstance?,
    val error: String? = null
) : PathEvaluation {
   // TODO : Re-think this.  EvaluatedEdge.element can be null in case of a failure.
   // Therefore, is it correct that "element" is non-null?
   override val element: Element = resultGraphElement!!

   companion object {
      fun success(evaluatedEdge: EvaluatableEdge, result: Element, resultValue: TypedInstance?): EvaluatedEdge {
         return EvaluatedEdge(evaluatedEdge, result, resultValue, error = null)
      }

      fun failed(edge: EvaluatableEdge, error: String): EvaluatedEdge {
         return EvaluatedEdge(edge, null, null, error)
      }
   }

   val wasSuccessful: Boolean = error == null

   val description: String by lazy {
      var desc = edge.description
      desc += if (wasSuccessful) {
         " (${resultGraphElement!!}) ✔"
      } else {
         " ✘ -> $error"
      }
      desc
   }

   override fun toString(): String = description
}
