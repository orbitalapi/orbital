package com.orbitalhq.query

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.orbitalhq.FactSetId
import com.orbitalhq.FactSets
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.fqn
import lang.taxi.accessors.ProjectionFunctionScope
import lang.taxi.expressions.Expression
import lang.taxi.expressions.ProjectingExpression
import lang.taxi.mutations.Mutation
import lang.taxi.services.operations.constraints.Constraint
import lang.taxi.types.ProjectionKind
import mu.KotlinLogging
import java.util.*


@Deprecated("Use TypedInstances instead")
data class Fact @JvmOverloads constructor(
   val typeName: String,
   val value: Any,
   val factSetId: FactSetId = FactSets.DEFAULT
) {
   val qualifiedName = typeName.fqn()

   companion object {
      fun fromTypedInstance(instance: TypedInstance, factSetId: FactSetId = FactSets.DEFAULT): Fact {
         return Fact(instance.type.paramaterizedName, instance, factSetId)
      }
   }


   fun toTypedInstance(schema:Schema):TypedInstance {
      return if (value is TypedInstance) {
         value
      } else {
         TypedInstance.from(schema.type(typeName), value, schema)
      }
   }
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
//@JsonDeserialize(`as` = TypeNameListQueryExpression::class)
// Removed the deserialization here, as I don't think it's used
// (all queries are now sent as TaxiQL).
// Will see what breaks.
interface QueryExpression

data class StreamJoiningExpression(val streamExpressions: List<TypeQueryExpression>) : QueryExpression


interface MutatingQueryExpression : QueryExpression {
   val mutation: Mutation?

   companion object {
      fun decorate(expression: QueryExpression?, mutation: Mutation?): QueryExpression {
         return when {
            expression == null && mutation == null -> error("Neither a query expression nor a mutation were provided")
            expression == null && mutation != null -> MutationOnlyExpression(mutation)
            expression != null && mutation == null -> expression
            expression != null && mutation != null -> QueryAndMutateExpression(expression, mutation)
            else -> error("Unhandled branch in constructing possibly mutating expression")
         }
      }
   }
}

data class ConstrainedTypeNameQueryExpression(
   val typeName: String,
   // Note: Not convinced this needs to be OutputConstraint (vs plain old
   // constraint). Revisit if this proves problematic
   val constraint: List<Constraint>
) : QueryExpression

data class ExpressionQuery(val expression: Expression, val legacyExpression: QueryExpression?): QueryExpression {

   val constraints: List<Constraint>
      get() {
         return when {
            legacyExpression is ConstrainedTypeNameQueryExpression -> legacyExpression.constraint
            else -> emptyList()
         }
      }
}

data class TypeQueryExpression(val type: Type) : QueryExpression

data class TypeNameQueryExpression(val typeName: String) : QueryExpression {
   init {
      logger.warn { "TypeNameQueryExpression shouldn't be called - prefer TypeQueryExpression where possible" }
   }

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   val qualifiedTypeNames: QualifiedName = typeName.fqn()
}

data class MutationOnlyExpression(override val mutation: Mutation) : MutatingQueryExpression
data class QueryAndMutateExpression(val query: QueryExpression, override val mutation: Mutation): MutatingQueryExpression

// TODO : Can we replace / deprecate in favour of taxi's ProjectingExpression?
data class ProjectedExpression(val source: QueryExpression, val projection: Projection): QueryExpression

// TODO : Can we replace / collapse with FieldProjection?
data class Projection(val type: Type, val scopedVars: List<ProjectionFunctionScope>, val projectingExpression: ProjectingExpression) {
   val projectionKind: ProjectionKind = projectingExpression.projection.projectionKind
}


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
   BUILD,
}

enum class ResultMode {
   /**
    * Raw results
    */
   RAW,

   /**
    * Exclude type information for each attribute in 'results'
    */
   @Deprecated("Use TYPED instead", replaceWith = ReplaceWith("ResultMode.TYPED"))
   SIMPLE,

   /**
    * Provide type metadata in results at a row level
    */
   TYPED,

   /**
    * Include type information for each attribute included in 'results'
    */
   VERBOSE;
}

// Used Built-in regression pack.
data class QueryHolder(val query: Any, val type: String = query::class.java.name)
