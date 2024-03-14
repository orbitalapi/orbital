package com.orbitalhq.query

import com.fasterxml.jackson.annotation.JsonInclude
import com.orbitalhq.schemas.OutputConstraint
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.taxi.TaxiConstraintConverter
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
   // Note: Not really convinced these need to be OutputConstraints (vs Constraints).
   // Revisit later
   val dataConstraints: List<OutputConstraint> = emptyList(),
   val projection: Projection? = null,
   val mutation: Mutation? = null
) {

   fun anonymousTypes(): Set<Type> {
      return this.type.anonymousTypes + (projection?.type?.anonymousTypes ?: emptySet())
   }

   companion object {
      private val logger = KotlinLogging.logger {}

      // Migrating logic here as an interim step to try to reduce
      // the number of classes involved in building a QuerySpecTypeNode
      fun buildConstraints(targetType: Type, schema: Schema, constraints: List<Constraint>):List<OutputConstraint> {
         val constraintProvider = TaxiConstraintConverter(schema)
         return constraintProvider.buildOutputConstraints(targetType,constraints)
      }

      // Trying to reduce the number of steps, and bypassing the QueryParser by moving that logic here.
      fun fromExpression(expression: Expression, schema: Schema):QuerySpecTypeNode {
         val targetType = expression.returnType
         val constraints = if (expression is TypeExpression) {
            buildConstraints(schema.type(targetType), schema, expression.constraints)
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


