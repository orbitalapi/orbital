package io.vyne.query

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.vyne.FactSetId
import io.vyne.FactSets


data class Fact(val typeName: String, val value: Any, val factSetId: FactSetId = FactSets.DEFAULT)

// TODO : facts should be QualifiedName -> TypedInstance, but need to get
// json deserialization working for that.
data class Query(val expression: QueryExpression, val facts: List<Fact> = emptyList(), val queryMode: QueryMode = QueryMode.DISCOVER) {
   constructor(queryString: QueryExpression, facts: Map<String, Any> = emptyMap(), queryMode: QueryMode = QueryMode.DISCOVER) : this(queryString, facts.map { Fact(it.key, it.value) }, queryMode)
   constructor(queryString: String, facts: Map<String, Any> = emptyMap(), queryMode: QueryMode = QueryMode.DISCOVER) : this(TypeNameQueryExpression(queryString), facts.map { Fact(it.key, it.value) }, queryMode)
}

@JsonDeserialize(using = QueryExpressionDeserializer::class)
@JsonSerialize(using = QueryExpressionSerializer::class)
interface QueryExpression

data class TypeNameQueryExpression(val typeName: String) : QueryExpression
data class TypeNameListQueryExpression(val typeNames: List<String>) : QueryExpression
// Note - this doesn't exist yet, but I'm leaving it here so I remember why I chose
// this object type over a simple string.
data class GraphQlQueryExpression(val shape: Map<String, Object>) : QueryExpression


enum class QueryMode {
   /**
    * Find a single value
    */
   DISCOVER,

   /**
    * Find all the values
    */
   GATHER
}
