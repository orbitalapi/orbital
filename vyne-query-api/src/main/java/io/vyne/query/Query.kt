package io.vyne.query

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.vyne.FactSetId
import io.vyne.FactSets
import io.vyne.schemas.Constraint
import io.vyne.schemas.OutputConstraint


data class Fact @JvmOverloads constructor(val typeName: String, val value: Any, val factSetId: FactSetId = FactSets.DEFAULT)

// TODO : facts should be QualifiedName -> TypedInstance, but need to get
// json deserialization working for that.
data class Query(
   val expression: QueryExpression,
   val facts: List<Fact> = emptyList(),
   val queryMode: QueryMode = QueryMode.DISCOVER,
   val resultMode: ResultMode = ResultMode.SIMPLE) {
   constructor(
      queryString: QueryExpression,
      facts: Map<String, Any> = emptyMap(),
      queryMode: QueryMode = QueryMode.DISCOVER,
      resultMode: ResultMode = ResultMode.SIMPLE)
      : this(queryString, facts.map { Fact(it.key, it.value) }, queryMode, resultMode)

   constructor(
      queryString: String,
      facts: Map<String, Any> = emptyMap(),
      queryMode: QueryMode = QueryMode.DISCOVER,
      resultMode: ResultMode = ResultMode.SIMPLE)
      : this(TypeNameQueryExpression(queryString), facts.map { Fact(it.key, it.value) }, queryMode, resultMode)
}

@JsonDeserialize(using = QueryExpressionDeserializer::class)
@JsonSerialize(using = QueryExpressionSerializer::class)
interface QueryExpression

data class ConstrainedTypeNameQueryExpression(val typeName:String,
                                              // Note: Not convinced this needs to be OutputConstraint (vs plain old
                                              // constraint). Revisit if this proves problematic
                                              val constraint:List<OutputConstraint>):QueryExpression
data class TypeNameQueryExpression(val typeName: String) : QueryExpression
data class TypeNameListQueryExpression(val typeNames: List<String>) : QueryExpression
// Note - this doesn't exist yet, but I'm leaving it here so I remember why I chose
// this object type over a simple string.
data class GraphQlQueryExpression(val shape: Map<String, Any>) : QueryExpression


enum class QueryMode {
   /**
    * Find a single value
    */
   DISCOVER,

   /**
    * Find all the values
    */
   GATHER,

   /**
    * Build an instance, using the data provided,
    * polyfilling where required
    */
   BUILD
}

enum class ResultMode {
   /**
    * Exclude type information for each attribute in 'results'
    */
   SIMPLE,
   /**
    * Include type information for each attribute included in 'results'
    */
   VERBOSE
}

