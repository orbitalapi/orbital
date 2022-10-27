package io.vyne.query.graph.edges

import arrow.core.Either
import io.vyne.models.MixedSources
import io.vyne.models.TypedCollection
import io.vyne.models.TypedNull
import io.vyne.models.TypedObject
import io.vyne.query.QueryContext
import io.vyne.schemas.Relationship
import lang.taxi.utils.getOrThrow

/**
 * Takes Array<T> and a property name, and maps that to Array<T.$propertyName>
 */
class ArrayMappingAttributeEvaluator : AttributeEvaluator(Relationship.CAN_ARRAY_MAP_TO) {
   override suspend fun evaluate(edge: EvaluatableEdge, context: QueryContext): EvaluatedEdge {
      val previousValue =
         requireNotNull(edge.previousValue) { "Cannot evaluate $relationship when previous value was null.  Work with me here!" }

      if (previousValue is TypedNull) {
         return edge.failure(
            previousValue,
            "Null was returned from previous edge: " + (edge.previous as EvaluatedEdge).description
         )
      }

      require(previousValue is TypedCollection) {
         "Cannot evaluate $relationship when the previous value isn't a TypedCollection - got ${previousValue::class.simpleName}"
      }

      val children = previousValue.mapIndexed { index, member ->
         require(member is TypedObject) {
            "Cannot evaluate $relationship when the collection contains member that aren't a TypedObject - got ${previousValue::class.simpleName} at $index"
         }
         getAttributeValue(pathToAttribute(edge), member, context)
      }

      val result = if (children.any { it.isLeft() }) {
         val errors = children.mapNotNull { if (it is Either.Left) it.value else null }
            .joinToString("; ")
         edge.failure(previousValue, errors)
      } else {
         val values = children.map { it.getOrThrow("This shouldn't be possible, we checked this for failure already") }
         edge.success(TypedCollection.from(values, MixedSources.singleSourceOrMixedSources(values)))
      }

      return result
   }
}
