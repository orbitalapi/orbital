package com.orbitalhq.query

import com.orbitalhq.Message
import com.orbitalhq.query.history.QuerySankeyChartRow
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.SavedQuery
import com.orbitalhq.schemas.toVyneQualifiedName
import lang.taxi.CompilationMessage
import lang.taxi.errors
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery

/**
 * The result of a parser request.
 * Contains both the query and the SavedQuery
 * metadata if successful, or error messages
 * if unsuccesful.
 *
 * Generally, the query is not yet saved, so we don't
 * have as much information as a SavedQuery
 */
data class ParsedQuery(
   val taxi: TaxiQLQueryString,
   val queryKind: SavedQuery.QueryKind?,
   // Unlike a saved query, the name could be null
   val name: QualifiedName?,
   val compilationMessages: List<CompilationMessage>,
   val queryPlan: QueryPlan
) {
   constructor(
      query: TaxiQlQuery,
      compilationMessages: List<CompilationMessage> = emptyList(),
      queryPlan: QueryPlan = QueryPlan.empty(),
   ) : this(
      query.source,
      SavedQuery.QueryKind.forQueryMode(query.queryMode),
      query.name.toVyneQualifiedName(),
      compilationMessages,
      queryPlan,
   )

   companion object {
      fun compilationFailed(taxi: TaxiQLQueryString, errors: List<CompilationMessage>): ParsedQuery {
         return ParsedQuery(
            taxi = taxi,
            queryKind = null,
            name = null,
            compilationMessages = errors,
            queryPlan = QueryPlan.empty()
         )
      }
   }

   val hasCompilationErrors = compilationMessages.errors().isNotEmpty()
   val hasQueryErrors = queryPlan.hasErrors
}

data class QueryPlan(
   val steps: List<QuerySankeyChartRow>,
   val queryExecutionMessages: List<Message>
) {
   companion object {
      fun empty(): QueryPlan = QueryPlan(emptyList(), emptyList())
   }
   val hasErrors = queryExecutionMessages.isNotEmpty()
}
