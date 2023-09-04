package com.orbitalhq.query.graph.edges

import arrow.core.getOrHandle
import arrow.core.left
import arrow.core.right
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.ConstructedQueryDataSource
import com.orbitalhq.query.QueryContext
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.QueryOperation
import com.orbitalhq.schemas.Relationship
import lang.taxi.utils.quotedIfNecessary

/**
 * Constructs a TaxiQL query to find a target type, by looking it up based on an @Id annotation.
 */
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
                        context.schema,
                        source = ConstructedQueryDataSource(listOf(edge.previousValue))
                    )
                )
            }.getOrHandle { it }
    }

    private fun getIdType(
        edge: EvaluatableEdge,
        operation: QueryOperation
    ): arrow.core.Either<EvaluatedEdge, QualifiedName> {
        // MP 24-Aug-23: Historically, this used to expect a single Id type present on the return type.
        // (eg., look for the type that uniquely identifies this object).
        // However, that excludes use-csaes where we're doing Id resolution table lookups, where there
        // are multiple fields, all which uniquely identify the entity.
        // Initially, we'd thought that multiple @Id annotations indicated a composite key.
        // However, I've decided to use a seperate annotation for composite keys (if/ when that usecase arises).
        // So, instead, we just use the inbound vertex to indicate the id type.
        return if (edge.previousValue != null) {
            edge.previousValue.type.name.right()
        } else {
            EvaluatedEdge.failed(
                edge,
                "No value was provided from the previous edge"
            ).left()
        }
// Previous impl:
//      val idTypes = operation.returnType.getAttributesWithAnnotation("Id".fqn())
//      return if (idTypes.size != 1) {
//         EvaluatedEdge.failed(
//            edge,
//            "An error occurred - expected to find a single Id type on ${operation.returnType.name.shortDisplayName}, but found ${idTypes.size}"
//         ).left()
//      } else {
//         idTypes.values.single().type.right()
//      }
    }
}
