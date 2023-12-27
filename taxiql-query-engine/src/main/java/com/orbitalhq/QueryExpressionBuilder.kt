package com.orbitalhq

import com.orbitalhq.query.ConstrainedTypeNameQueryExpression
import com.orbitalhq.query.MutatingQueryExpression
import com.orbitalhq.query.ProjectionAnonymousTypeProvider
import com.orbitalhq.query.QueryExpression
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
 */
class QueryExpressionBuilder {
   fun build(taxiQl: TaxiQlQuery, schema:Schema):QueryExpression {
      val constraintProvider = TaxiConstraintConverter(schema)
      val queryExpressions = taxiQl.typesToFind.map { discoveryType ->

         val targetType = when {
            discoveryType.anonymousType != null -> ProjectionAnonymousTypeProvider.toVyneAnonymousType(discoveryType.anonymousType!!, schema)
            StreamType.isStreamTypeName(discoveryType.typeName) && UnionType.isUnionType(discoveryType.type.typeParameters()[0]) -> {
               val unionType = ProjectionAnonymousTypeProvider.toVyneStreamOfAnonymousType(discoveryType.type, schema)
               unionType
            }
            else -> schema.type(discoveryType.typeName.toVyneQualifiedName())
         }
         val expression = if (discoveryType.constraints.isNotEmpty()) {
            val constraints = constraintProvider.buildOutputConstraints(targetType, discoveryType.constraints)
            ConstrainedTypeNameQueryExpression(targetType.name.parameterizedName, constraints)
         } else {
            TypeQueryExpression(targetType)
         }

         expression
      }

      val expression: QueryExpression = when {
         queryExpressions.size > 1 -> {
            val streamJoin =  (queryExpressions.all { it is TypeQueryExpression && it.type.isStream })
            require(streamJoin) { "Multiple source types are only supported when joining streams" }
            StreamJoiningExpression(queryExpressions as List<TypeQueryExpression>)
               .applyProjection(taxiQl.projectedType, taxiQl.projectionScope, schema)
         }
         queryExpressions.size == 1 -> queryExpressions.first().let { expression ->
            expression.applyProjection(taxiQl.projectedType, taxiQl.projectionScope, schema)
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
