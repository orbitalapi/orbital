package io.osmosis.polymer.query.graph

import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.query.QueryContext
import io.osmosis.polymer.schemas.Link
import io.osmosis.polymer.schemas.Path
import io.osmosis.polymer.schemas.Relationship
import io.osmosis.polymer.utils.log
import org.springframework.stereotype.Component

data class EvaluatedLink(val link: Link, val startingPoint: TypedInstance, val result: TypedInstance?, val error: String? = null) {
   companion object {
      fun success(link: Link, startingPoint: TypedInstance, result: TypedInstance): EvaluatedLink {
         return EvaluatedLink(link, startingPoint, result, null)
      }

      fun failed(link: Link, startingPoint: TypedInstance, error: String): EvaluatedLink {
         return EvaluatedLink(link, startingPoint, null, error)
      }
   }

   val wasSuccessful: Boolean = error == null
}

data class EvaluatedPath(val path: Path, val startingPoint: TypedInstance, val evaluatedLinks: List<EvaluatedLink>) {

}

interface LinkEvaluator {
   val relationship: Relationship
   fun evaluate(link: Link, startingPoint: TypedInstance, context: QueryContext): EvaluatedLink
}

@Component
class PathEvaluator(linkEvaluators: List<LinkEvaluator>) {
   private val evaluators = linkEvaluators.associateBy { it.relationship }
   fun evaluate(path: Path, startingPoint: TypedInstance, context: QueryContext): EvaluatedPath {
      log().debug("Evaluating path: ${path.description}")

      var linkStartingPoint = startingPoint
      val evaluatedLinks = mutableListOf<EvaluatedLink>()
      path.links.takeWhile { link ->
         val evaluator = evaluators[link.relationship] ?: throw IllegalArgumentException("No link evaluator defined for relationship ${link.relationship}")
         log().debug("Evaluating $link from $linkStartingPoint")
         val linkEvaluation = evaluator.evaluate(link, linkStartingPoint, context)
         evaluatedLinks.add(linkEvaluation)
         log().debug("Link evaluated: $linkEvaluation")
         if (linkEvaluation.wasSuccessful) {
            linkStartingPoint = linkEvaluation.result!!
         } else {
            log().debug("Evaluating $link from $linkStartingPoint failed: ${linkEvaluation.error}")
         }

         // Continue if...
         linkEvaluation.wasSuccessful
      }
      return EvaluatedPath(path, startingPoint, evaluatedLinks)
   }

}
