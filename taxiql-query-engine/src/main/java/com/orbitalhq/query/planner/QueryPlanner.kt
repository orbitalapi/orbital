package com.orbitalhq.query.planner

import com.orbitalhq.query.QueryExpression
import com.orbitalhq.schemas.Schema
import lang.taxi.query.TaxiQlQuery

/**
 * A query planner will be responsible for interrogating a TaxiQL query, optimizing
 * and designing the most efficient query execution path.
 *
 * In time, we will implement planning and optimization.
 *
 * Currently, the planner only returns metadata.
 * TODO : Finish this comment when the class is implemented!
 *
 */
class QueryPlanner(cacheSize: Int = 50) {
   private val queryExpressionBuilder = QueryExpressionBuilder(this)
   private val metadataBuilder = QueryMetadataBuilder(cacheSize)
   fun buildMetadata(taxiQlQuery: TaxiQlQuery, schema: Schema): QueryPlanMetadata =
      metadataBuilder.buildMetadata(taxiQlQuery, schema)

   /**
    * Returns a QueryExpression - which is the top-level object that
    * Orbital uses to execute a TaxiQL Query.
    */
   fun buildQueryExpression(taxiQl: TaxiQlQuery, querySchema: Schema): QueryExpression {
      return queryExpressionBuilder.build(taxiQl, querySchema)
   }

}

/**
 * The hashcode of a QueryPlanCacheKey.
 * Lighterweight in memory than storing the actual cache key
 */
typealias QueryPlanCacheKeyHashCode = Int

data class QueryPlanCacheKey(val query: TaxiQlQuery, val schema: Schema)

