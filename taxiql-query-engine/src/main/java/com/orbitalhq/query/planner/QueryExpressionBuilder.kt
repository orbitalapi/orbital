package com.orbitalhq.query.planner

import com.orbitalhq.applyProjection
import com.orbitalhq.query.ConstrainedTypeNameQueryExpression
import com.orbitalhq.query.ExpressionQuery
import com.orbitalhq.query.MutatingQueryExpression
import com.orbitalhq.query.ProjectionAnonymousTypeProvider
import com.orbitalhq.query.QueryExpression
import com.orbitalhq.query.QuerySpecTypeNode
import com.orbitalhq.query.TypeQueryExpression
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.toVyneQualifiedName
import lang.taxi.query.DiscoveryType
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery
import lang.taxi.types.StreamType
import lang.taxi.types.UnionType
import mu.KotlinLogging

/**
 * Converts the TaxiQL query into a QueryExpression,
 * which is what Orbital uses internally
 * to execute the query.
 *
 * TODO : This will fold into the Query Planner eventually,
 * and currently has mixed responsibilities with it.
 */
class QueryExpressionBuilder(private val queryPlanner: QueryPlanner) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   /**
    * Constructs a QueryExpression for the provided TaxiQL query.
    * Additionally, it may rewrite the query, to do things like satisfy streaming requirements, etc.
    * Returns the QueryExpression, along with the amended (if applicable) TaxiQlQuery and Schema.
    * (If the query wasn't rewritten, returns the original query and schema)
    */
   fun build(taxiQl: TaxiQlQuery, schema: Schema): Triple<QueryExpression,TaxiQlQuery, Schema> {
      val queryMetadata = queryPlanner.buildMetadata(taxiQl, schema)

      val queryExpression = taxiQl.discoveryType?.let { discoveryType ->

         val targetType = when {
            discoveryType.type.anonymous -> ProjectionAnonymousTypeProvider.toVyneAnonymousType(
               discoveryType.type,
               schema
            )

            // The user has requested a union steam type.
            // eg: stream { A | B } as { ... }
            // This was the original way of joining streams.
            StreamType.isStreamTypeName(discoveryType.typeName) && UnionType.isUnionType(discoveryType.expression.returnType.typeParameters()[0]) -> {
               val unionType = ProjectionAnonymousTypeProvider.toVyneStreamOfAnonymousType(discoveryType.type, schema)
               unionType
            }

            // The user has requested a single stream type, but within the projection,
            // there are other streams required.
            // This is another way of joining streams.
            // Here, we rewrite the discovery type from Stream<A> to Stream<A|B>
            // as if the user had written:
            // stream { A | B }
            StreamType.isStreamTypeName(discoveryType.typeName) && queryMetadata.minimumStreamOperations.size > 1 -> {
               val amendedTaxiQL = appendMissingStreamSourcesToTaxiQL(queryMetadata, discoveryType, taxiQl)
               if (amendedTaxiQL == taxiQl.source) {
                  schema.type(discoveryType.typeName.toVyneQualifiedName())
               } else {
                  logger.info { "The provided query has been rewritten to:\n$amendedTaxiQL" }
                  val (amendedQuery, _, amendedSchema) = schema.parseQuery(amendedTaxiQL)
                  // Now build the expression against the amended query
                  return build(amendedQuery, amendedSchema)

               }
            }

            else -> schema.type(discoveryType.typeName.toVyneQualifiedName())
         }
         val legacyExpression = when {
            discoveryType.constraints.isNotEmpty() -> {
               val constraints = QuerySpecTypeNode.buildConstraints(targetType, schema, discoveryType.constraints)
               ConstrainedTypeNameQueryExpression(targetType.name.parameterizedName, constraints)
            }

            else -> TypeQueryExpression(targetType)
         }
         ExpressionQuery(discoveryType.expression, legacyExpression)
      }?.let { expressionQuery ->
         // Handle projections
         expressionQuery.applyProjection(taxiQl.projectedType, taxiQl.projectionScopeVars, schema)
      }.let { possibleQueryExpression ->
         // At this point we have either:
         // Mutation -only query.
         // Query-only query.
         // Query-then-mutate query.
         // Decorate encapsulates those and returns the correct expression
         MutatingQueryExpression.decorate(possibleQueryExpression, taxiQl.mutation)
      }
      return Triple(queryExpression, taxiQl, schema)
   }

   /**
    * Given:
    * stream { Foo } as {
    *    ...
    *    b : Bar // comes from another stream
    * }
    *
    * then will work out the missing stream sources, and append them, rewriting the query to:
    *
    * stream { Foo | Bar } as { ... }
    *
    */
   private fun appendMissingStreamSourcesToTaxiQL(
      queryMetadata: QueryPlanMetadata,
      discoveryType: DiscoveryType,
      taxiQl: TaxiQlQuery
   ): TaxiQLQueryString {
      val streamSourceTypes = queryMetadata.possibleStreamTypes.map { it.taxiType }
      val presentStreamSourceTypes = if (UnionType.isUnionType(discoveryType.type.typeParameters()[0])) {
         val unionType = discoveryType.type.typeParameters()[0] as UnionType
         unionType.types
      } else {
         // Stream type
         listOf(discoveryType.type.typeParameters()[0])
      }
      val missingStreamSourceTypes = streamSourceTypes.filterNot { requiredStreamSourceType ->
         // It's valid to request
         // stream { A } to request a stream of all subtypes of A.
         // These don't need to be rewritten.
         // (I think - tests are currently passing, and no real-world usage of this scenario yet).
         // Therefore, check for assignability, rather than equality, when checking for current stream sources
         presentStreamSourceTypes.any { presentStreamSourceType -> requiredStreamSourceType.isAssignableTo(presentStreamSourceType)}
      }
      if (missingStreamSourceTypes.isEmpty()) {
         return taxiQl.source
      }
      val rewriter = TaxiQlRewriter()
      val amendedTaxiQL = missingStreamSourceTypes.fold(taxiQl.source) { taxiQL, type ->
         rewriter.appendStreamSource(
            taxiQL,
            type.toQualifiedName().parameterizedName
         )
      }
      return amendedTaxiQL
   }
}
