package com.orbitalhq.query

import com.fasterxml.jackson.annotation.JsonInclude
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.expressions.Expression
import lang.taxi.expressions.TypeExpression
import lang.taxi.mutations.Mutation
import lang.taxi.services.operations.constraints.Constraint
import lang.taxi.types.PrimitiveType
import mu.KotlinLogging

/**
 * Defines a search that is executed, along with it's projection.
 *
 * Part of long-term technical debt.
 *
 * This object is created by:
 * TaxiQl -> QueryExpressionBuilder -> QueryExpression -> QueryParser -> QuerySpecTypeNode
 *
 * However, most of the above is redundant, and exists for legacy reasons.
 *
 * Long term, this is to be deprecated, and replaced by searching
 * against expressions
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class QuerySpecTypeNode(
   val type: Type,
   val expression: Expression? = null,
   @Deprecated("Not used, not required")
   // Note: Currently using children to wrap nested nodes when joining multiple streams (one child for each contributing stream).
   val children: Set<QuerySpecTypeNode> = emptySet(),
   val mode: QueryMode = QueryMode.DISCOVER,
   val dataConstraints: List<Constraint> = emptyList(),
   val projection: Projection? = null,
   val mutation: Mutation? = null
) {

   fun anonymousTypes(): Set<Type> {
      return this.type.anonymousTypes + (projection?.type?.anonymousTypes ?: emptySet())
   }

   companion object {
      private val logger = KotlinLogging.logger {}

      // Trying to reduce the number of steps, and bypassing the QueryParser by moving that logic here.
      fun fromExpression(expression: Expression, schema: Schema):QuerySpecTypeNode {
         val targetType = expression.returnType
         val constraints = if (expression is TypeExpression) {
            expression.constraints
         } else emptyList()
         return QuerySpecTypeNode(
            schema.type(expression.returnType),
            expression,
            dataConstraints = constraints
         )
      }
   }

   init {
      if (type.isCollection && type.collectionTypeName!!.fullyQualifiedName == PrimitiveType.ANY.qualifiedName) {
         logger.warn { "Performing a search for Any[] is likely a bug" }
      }
   }

   val description = type.name.shortDisplayName
}


