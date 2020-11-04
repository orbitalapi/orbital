package io.vyne.query

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.vyne.FactSetId
import io.vyne.FactSets
import io.vyne.schemas.OutputConstraint
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.fqn
import kotlin.reflect.KClass


data class Fact @JvmOverloads constructor(val typeName: String, val value: Any, val factSetId: FactSetId = FactSets.DEFAULT) {
   val qualifiedName = typeName.fqn()
}

// TODO : facts should be QualifiedName -> TypedInstance, but need to get
// json deserialization working for that.
data class Query(
   val expression: QueryExpression,
   val facts: List<Fact> = emptyList(),
   val queryMode: QueryMode = QueryMode.DISCOVER) {
   constructor(
      queryString: QueryExpression,
      facts: Map<String, Any> = emptyMap(),
      queryMode: QueryMode = QueryMode.DISCOVER)
      : this(queryString, facts.map { Fact(it.key, it.value) }, queryMode)

   constructor(
      queryString: String,
      facts: Map<String, Any> = emptyMap(),
      queryMode: QueryMode = QueryMode.DISCOVER)
      : this(TypeNameQueryExpression(queryString), facts.map { Fact(it.key, it.value) }, queryMode)
}

//@JsonDeserialize(using = QueryExpressionDeserializer::class)
//@JsonSerialize(using = QueryExpressionSerializer::class)
// For now, all UI calls send a type TypeNameListQueryExpression.
@JsonDeserialize(`as` = TypeNameListQueryExpression::class)
interface QueryExpression

data class ConstrainedTypeNameQueryExpression(val typeName: String,
   // Note: Not convinced this needs to be OutputConstraint (vs plain old
   // constraint). Revisit if this proves problematic
                                              val constraint: List<OutputConstraint>) : QueryExpression

data class TypeNameQueryExpression(val typeName: String) : QueryExpression {
   val qualifiedTypeNames: QualifiedName = typeName.fqn()
}

data class TypeNameListQueryExpression(val typeNames: List<String>) : QueryExpression {
   val qualifiedTypeNames = typeNames.map { it.fqn() }
}

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

enum class ResultMode(val viewClass: KClass<out ResultView>) {
   /**
    * Raw results
    */
   RAW(SimpleResultView::class),

   /**
    * Exclude type information for each attribute in 'results'
    */
   SIMPLE(SimpleResultView::class),

   /**
    * Include type information for each attribute included in 'results'
    */
   VERBOSE(VerboseResultView::class)
}
interface ResultView
interface SimpleResultView : ResultView
interface VerboseResultView : ResultView
