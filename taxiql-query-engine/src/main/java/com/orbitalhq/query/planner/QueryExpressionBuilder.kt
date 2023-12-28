package com.orbitalhq.query.planner

import com.orbitalhq.applyProjection
import com.orbitalhq.query.ConstrainedTypeNameQueryExpression
import com.orbitalhq.query.MutatingQueryExpression
import com.orbitalhq.query.ProjectionAnonymousTypeProvider
import com.orbitalhq.query.QueryExpression
import com.orbitalhq.query.RewrittenTypeQueryExpression
import com.orbitalhq.query.StreamJoiningExpression
import com.orbitalhq.query.TypeQueryExpression
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.taxi.TaxiConstraintConverter
import com.orbitalhq.schemas.toVyneQualifiedName
import lang.taxi.query.TaxiQlQuery
import lang.taxi.types.StreamType
import lang.taxi.types.UnionType

/**
 * Converts the TaxiQL query into a QueryExpression,
 * which is what Orbital uses internally
 * to execute the query.
 *
 * TODO : This will fold into the Query Planner eventually,
 * and currently has mixed responsibilities with it.
 */
class QueryExpressionBuilder(private val queryPlanner: QueryPlanner) {
   fun build(taxiQl: TaxiQlQuery, schema: Schema): QueryExpression {
      val constraintProvider = TaxiConstraintConverter(schema)
      val queryMetadata = queryPlanner.buildMetadata(taxiQl, schema)
      val queryExpressions = taxiQl.typesToFind.map { discoveryType ->

         val (targetType, amendedProjectionScope) = when {
            discoveryType.anonymousType != null -> ProjectionAnonymousTypeProvider.toVyneAnonymousType(
               discoveryType.anonymousType!!,
               schema
            ) to null

            // The user has requested a union steam type.
            // eg: stream { A | B } as { ... }
            // This was the original way of joining streams.
            StreamType.isStreamTypeName(discoveryType.typeName) && UnionType.isUnionType(discoveryType.type.typeParameters()[0]) -> {
               val unionType = ProjectionAnonymousTypeProvider.toVyneStreamOfAnonymousType(discoveryType.type, schema)
               unionType to null
            }

            // The user has requested a single stream type, but within the projection,
            // there are other streams required.
            // This is another way of joining streams.
            // Here, we rewrite the discovery type from Stream<A> to Stream<A|B>
            // as if the user had written:
            // stream { A | B }
            // This allows the downstream "stream
            StreamType.isStreamTypeName(discoveryType.typeName) && queryMetadata.candidateStreamOperations.size > 1 -> {
               val taxiUnionType = UnionType(
                  queryMetadata.possibleStreamTypes.map { it.taxiType },
                  null,
                  emptyList(),
                  taxiQl.compilationUnits.first()
               )
               val streamType = StreamType.of(taxiUnionType)

               val vyneUnionType = ProjectionAnonymousTypeProvider.toVyneStreamOfAnonymousType(streamType, schema)
               // Originally, the query was defined as `stream { A } as { .... }
               // However, we've rewritten it to `stream { A | B } as { ... },
               // so the projection scope also needs to update.
               val amendedProjectionScope = taxiQl.projectionScope?.copy(type = streamType)
               vyneUnionType to amendedProjectionScope


            }

            else -> schema.type(discoveryType.typeName.toVyneQualifiedName()) to null
         }
         val expression = when {
            discoveryType.constraints.isNotEmpty() -> {
               val constraints = constraintProvider.buildOutputConstraints(targetType, discoveryType.constraints)
               ConstrainedTypeNameQueryExpression(targetType.name.parameterizedName, constraints)
            }

            amendedProjectionScope != null -> RewrittenTypeQueryExpression(targetType, amendedProjectionScope)
            else -> TypeQueryExpression(targetType)
         }

         expression
      }

      val expression: QueryExpression = when {
         queryExpressions.size > 1 -> {
            error("Is this code ever hit?")
            val streamJoin = (queryExpressions.all { it is TypeQueryExpression && it.type.isStream })
            require(streamJoin) { "Multiple source types are only supported when joining streams" }
            StreamJoiningExpression(queryExpressions as List<TypeQueryExpression>)
               .applyProjection(taxiQl.projectedType, taxiQl.projectionScope, schema)
         }

         queryExpressions.size == 1 -> queryExpressions.first().let { expression ->
            // If we rewrote the query type above, we need to also rewrite the projection
            if (expression is RewrittenTypeQueryExpression) {
               val amendedProjectionScope = expression.amendedProjectionScope
               expression
                  // We actually want to return a TypeQueryExpression - which is what everything else
                  // knows how to use.
                  .toTypeQueryExpression()
                  .applyProjection(taxiQl.projectedType, amendedProjectionScope, schema)
            } else {
               expression.applyProjection(taxiQl.projectedType, taxiQl.projectionScope, schema)
            }

         }

         else -> null
      }.let { possibleQueryExpression ->
         // At this point we have either:
         // Mutation -only query.
         // Query-only query.
         // Query-then-mutate query.
         // Decorate encapsulates those and returns the correct expression
         MutatingQueryExpression.decorate(possibleQueryExpression, taxiQl.mutation)
      }
      return expression
   }
}
