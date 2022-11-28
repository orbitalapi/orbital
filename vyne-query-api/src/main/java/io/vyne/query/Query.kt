package io.vyne.query

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.vyne.FactSetId
import io.vyne.FactSets
import io.vyne.schemas.OutputConstraint
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import lang.taxi.accessors.ProjectionFunctionScope
import java.util.UUID
import kotlin.reflect.KClass


data class Fact @JvmOverloads constructor(
   val typeName: String,
   val value: Any,
   val factSetId: FactSetId = FactSets.DEFAULT
) {
   val qualifiedName = typeName.fqn()
}

// TODO : facts should be QualifiedName -> TypedInstance, but need to get
// json deserialization working for that.
data class Query(
   val expression: QueryExpression,
   val facts: List<Fact> = emptyList(),
   val queryMode: QueryMode = QueryMode.DISCOVER,
   val queryId: String = UUID.randomUUID().toString()
) {
   constructor(
      queryString: QueryExpression,
      facts: Map<String, Any> = emptyMap(),
      queryMode: QueryMode = QueryMode.DISCOVER,
      queryId: String = UUID.randomUUID().toString()
   )
      : this(queryString, facts.map { Fact(it.key, it.value) }, queryMode, queryId)

   constructor(
      queryString: String,
      facts: Map<String, Any> = emptyMap(),
      queryMode: QueryMode = QueryMode.DISCOVER,
      queryId: String = UUID.randomUUID().toString()
   )
      : this(TypeNameQueryExpression(queryString), facts.map { Fact(it.key, it.value) }, queryMode, queryId)
}

//@JsonDeserialize(using = QueryExpressionDeserializer::class)
//@JsonSerialize(using = QueryExpressionSerializer::class)
// For now, all UI calls send a type TypeNameListQueryExpression.
@JsonDeserialize(`as` = TypeNameListQueryExpression::class)
interface QueryExpression

data class ConstrainedTypeNameQueryExpression(
   val typeName: String,
   // Note: Not convinced this needs to be OutputConstraint (vs plain old
   // constraint). Revisit if this proves problematic
   val constraint: List<OutputConstraint>
) : QueryExpression

data class TypeNameQueryExpression(val typeName: String) : QueryExpression {
   val qualifiedTypeNames: QualifiedName = typeName.fqn()
}
data class ProjectedExpression(val source: QueryExpression, val projection: Projection) : QueryExpression

data class Projection(val type: Type, val scope: ProjectionFunctionScope?)

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
   @Deprecated("findOne is no longer supported.  Use Gather - which is equivalent of old findAll {}, and current find {} ")
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
   @Deprecated("Use TYPED instead", replaceWith = ReplaceWith("ResultMode.TYPED"))
   SIMPLE(SimpleResultView::class),

   /**
    * Provide type metadata in results at a row level
    */
   TYPED(SimpleResultView::class),
   /**
    * Include type information for each attribute included in 'results'
    */
   @Deprecated("Use Simple instead, this contains too much data")
   VERBOSE(VerboseResultView::class);


}

interface ResultView
interface SimpleResultView : ResultView
interface VerboseResultView : ResultView

// Used Built-in regression pack.
data class QueryHolder(val query: Any, val type: String = query::class.java.name)
