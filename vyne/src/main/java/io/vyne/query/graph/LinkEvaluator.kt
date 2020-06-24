package io.vyne.query.graph

import io.vyne.models.TypedInstance
import io.vyne.query.QueryContext
import io.vyne.schemas.Link
import io.vyne.schemas.Relationship

data class EvaluatedLink(val link: Link, val startingPoint: TypedInstance, val result: TypedInstance?, val error: String? = null) {
   companion object {
      fun success(link: Link, startingPoint: TypedInstance, result: TypedInstance): EvaluatedLink {
         return EvaluatedLink(link, startingPoint, result, null)
      }

      fun failed(link: Link, startingPoint: TypedInstance, error: String): EvaluatedLink {
         return EvaluatedLink(link, startingPoint, null, error)
      }
   }
}

interface LinkEvaluator {
   val relationship: Relationship
   fun evaluate(link: Link, startingPoint: TypedInstance, context: QueryContext): EvaluatedLink
}

