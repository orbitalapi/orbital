package io.vyne.query.graph.edges

import arrow.core.getOrHandle
import arrow.core.left
import arrow.core.right
import io.vyne.models.TypedInstance
import io.vyne.query.QueryContext
import io.vyne.query.VyneQlGrammar
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.QueryOperation
import io.vyne.schemas.Relationship
import io.vyne.schemas.fqn
import lang.taxi.utils.quotedIfNecessary

class QueryBuildingEvaluator : EdgeEvaluator {
   companion object {
      fun buildQuery(typeToSelect: QualifiedName, idType: QualifiedName, idValue: Any): String {
         return """
find { ${typeToSelect.parameterizedName}( ${idType.parameterizedName} == ${idValue.quotedIfNecessary()} ) }
""".trim()
      }
   }

   override val relationship: Relationship = Relationship.CAN_CONSTRUCT_QUERY

   override suspend fun evaluate(edge: EvaluatableEdge, context: QueryContext): EvaluatedEdge {
      val operation = edge.vertex2.instanceValue as QueryOperation
      val returnTypeName = operation.returnType.name
      val idValue = edge.previousValue?.value ?: return edge.failure(
         null,
         "Cannot construct a query as the provided Id value was null"
      )
      return getIdType(edge, operation)
         .map { idType ->
            val taxiQlQuery = buildQuery(
               returnTypeName,
               idType,
               idValue
            )
            edge.success(
               TypedInstance.from(
                  context.schema.type(VyneQlGrammar.QUERY_TYPE_NAME),
                  taxiQlQuery,
                  context.schema
               )
            )
         }.getOrHandle { it }
   }

   private fun getIdType(
      edge: EvaluatableEdge,
      operation: QueryOperation
   ): arrow.core.Either<EvaluatedEdge, QualifiedName> {
      val idTypes = operation.returnType.getAttributesWithAnnotation("Id".fqn())
      return if (idTypes.size != 1) {
         EvaluatedEdge.failed(
            edge,
            "An error occurred - expected to find a single Id type on ${operation.returnType.name.shortDisplayName}, but found ${idTypes.size}"
         ).left()
      } else {
         idTypes.values.single().type.right()
      }
   }
}
