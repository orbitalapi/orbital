package com.orbitalhq.query

import com.orbitalhq.Message
import com.orbitalhq.query.history.QueryPlanDiagramData
import com.orbitalhq.query.history.QuerySankeyChartRow
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.SavedQuery
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.fqn
import com.orbitalhq.schemas.toVyneQualifiedName
import lang.taxi.CompilationMessage
import lang.taxi.errors
import lang.taxi.query.Parameter
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery

/**
 * Metadata about a query.
 * Does not contain the actual query itself, but
 * various metadata elements (such as a query plan, the return type, etc).
 *
 * Not sure what to call this - used to be called a ParsedQuery
 * because it contained the actual TaxiQL query object.
 * However, it's intended this is returned to the UI, and the TaxiQL query object
 * is a deep object, right down to the compilation units.
 *
 */
data class QueryParseMetadata(
   val taxi: TaxiQLQueryString,
   val queryKind: SavedQuery.QueryKind?,
   // Unlike a saved query, the name could be null
   val name: QualifiedName?,
   val compilationMessages: List<CompilationMessage>,
   val queryPlan: QueryPlan,
   val returnType: Type?,
   val parameters: List<QueryParameterDto>,
   val facts: List<QueryParameterDto>
) {
   companion object {
      fun fromQuery(
         query: TaxiQlQuery,
         compilationMessages: List<CompilationMessage> = emptyList(),
         queryPlan: QueryPlan = QueryPlan.empty(),
         schema: Schema,
      ): QueryParseMetadata {
         return QueryParseMetadata(
            query.source,
            SavedQuery.QueryKind.forQueryMode(query.queryMode),
            query.name.toVyneQualifiedName(),
            compilationMessages,
            queryPlan,
            schema.type(query.returnType),
            query.parameters.map { QueryParameterDto.fromParameter(it) },
            query.facts.map { QueryParameterDto.fromParameter(it) }
         )
      }

      fun compilationFailed(taxi: TaxiQLQueryString, errors: List<CompilationMessage>): QueryParseMetadata {
         return QueryParseMetadata(
            taxi = taxi,
            queryKind = null,
            name = null,
            compilationMessages = errors,
            queryPlan = QueryPlan.empty(),
            returnType = null,
            parameters = emptyList(),
            facts = emptyList()
         )
      }
   }

   val hasCompilationErrors = compilationMessages.errors().isNotEmpty()
   val hasQueryErrors = queryPlan.hasErrors
}

data class QueryPlan(
   val steps: List<QuerySankeyChartRow>,
   val queryExecutionMessages: List<Message>,
   val diagramData: QueryPlanDiagramData = QueryPlanDiagramData.EMPTY

) {
   companion object {
      fun empty(): QueryPlan = QueryPlan(emptyList(), emptyList())
   }

   val hasErrors = queryExecutionMessages.isNotEmpty()
}


/**
 * A lightweight version of lang.taxi.query.Parameter,
 * which is present on a TaxiQLQuery. Intended to ship to the UI,
 * so contains a small amount of relevant data.
 *
 * Add as required.
 */
data class QueryParameterDto(
   val name: String,
   val type: QualifiedName
) {
   companion object {
      fun fromParameter(param: Parameter): QueryParameterDto = QueryParameterDto(
         param.name,
         param.type.qualifiedName.fqn()
      )
   }
}
