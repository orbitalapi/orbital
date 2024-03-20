package com.orbitalhq.connectors.nosql.mongodb

import com.orbitalhq.connectors.getTypesToFind
import com.orbitalhq.schemas.Schema
import lang.taxi.TaxiDocument
import lang.taxi.expressions.LiteralExpression
import lang.taxi.expressions.OperatorExpression
import lang.taxi.expressions.TypeExpression
import lang.taxi.query.TaxiQlQuery
import lang.taxi.services.operations.constraints.Constraint
import lang.taxi.services.operations.constraints.ExpressionConstraint
import lang.taxi.types.FieldReference
import lang.taxi.types.FormulaOperator
import lang.taxi.types.ObjectType
import lang.taxi.types.Type
import org.springframework.data.mongodb.core.query.Criteria

class MongoCriteriaGenerator(private val taxiSchema: TaxiDocument) {
   constructor(schema: Schema) : this(schema.taxi)

   fun crtieriaFor(query: TaxiQlQuery): List<Criteria> {
      val typesToFind = getTypesToFind(query, taxiSchema)
      val typesToCollectionNames =  MongoQueryHelpers.getCollectionNames(typesToFind)
      if (typesToCollectionNames.size > 1) {
         error("Mongo Joins are not yet supported - can only select from a single collection")
      }
      return  typesToFind.filter { (_, discoveryType) -> discoveryType.constraints.isNotEmpty() }
         .map { (type, discoveryType) ->
            // Pretty sure that now we'll only ever receive a single constraint,
            // which more often than not is an Operator Constraint.
            // (Which can nest mulitple operator constratins under it)
            require(discoveryType.constraints.size == 1) { "Expected to find a single constraint (which could be a compound operation expression).  Instead, found ${discoveryType.constraints.size}" }
            val collectionName = typesToCollectionNames[type]!!
            buildMongoConstraint(
               type,
               discoveryType.constraints.single(),
            )
         }


   }

   private fun buildMongoConstraint(
      type: ObjectType,
      constraint: Constraint
   ): Criteria {
      return when (constraint) {
         is ExpressionConstraint -> buildMongoConstraint(
            type,
            constraint
         )
         else -> error("Sql constraints not supported for constraint type ${constraint::class.simpleName}")
      }
   }

   private fun buildMongoConstraint(
      type: ObjectType,
      constraint: ExpressionConstraint
   ): Criteria {
      return when (val expression = constraint.expression) {
         is OperatorExpression -> buildSqlConstraintFromOperatorExpression(
            type,
            expression,
         )
         else -> error("Unsupported expression type: ${expression::class.simpleName}")
      }
   }

   private fun buildSqlConstraintFromOperatorExpression(
      type: ObjectType,
      expression: OperatorExpression
   ): Criteria {
      return when {
         expression.lhs is TypeExpression && expression.rhs is LiteralExpression -> {
            buildTypeToLiteralExpression(
               expression.lhs as TypeExpression,
               expression.rhs as LiteralExpression,
               expression.operator,
               type
            )
         }

         expression.lhs is OperatorExpression && expression.rhs is OperatorExpression -> {
            buildCompoundExpression(
               expression.lhs as OperatorExpression,
               expression.rhs as OperatorExpression,
               expression.operator,
               type
            )
         }

         else -> error("Sql generation not implemented for operation expression ${expression.lhs::class.simpleName} and ${expression.rhs::class.simpleName}")
      }
   }

   private fun buildCompoundExpression(
      lhsExpression: OperatorExpression,
      rhsExpression: OperatorExpression,
      operator: FormulaOperator,
      type: ObjectType
   ): Criteria {
      val lhsCriteria = buildSqlConstraintFromOperatorExpression(
         type,
         lhsExpression
      )
      val rhsCriteria = buildSqlConstraintFromOperatorExpression(
         type, rhsExpression
      )
      return when (operator) {
         FormulaOperator.LogicalAnd -> Criteria().andOperator(lhsCriteria, rhsCriteria)
         //lhsCondition.and(rhsCondition)
         FormulaOperator.LogicalOr -> Criteria().orOperator(lhsCriteria, rhsCriteria)
         else -> error("Operator $operator not supported for compound expressions")
      }
   }

   private fun buildTypeToLiteralExpression(
      lhs: TypeExpression,
      rhs: LiteralExpression,
      operator: FormulaOperator,
      type: ObjectType
   ): Criteria {
      val fieldReference = getSingleField(type, lhs.type)
      val fieldName = fieldReference.path.single().name
      val condition = when (operator) {
         FormulaOperator.Equal -> Criteria.where(fieldName).`is`(rhs.value)
         FormulaOperator.NotEqual -> Criteria.where(fieldName).ne(rhs.value)
         FormulaOperator.LessThan -> Criteria.where(fieldName).lt(rhs.value)
         FormulaOperator.LessThanOrEqual -> Criteria.where(fieldName).lte(rhs.value)
         FormulaOperator.GreaterThan -> Criteria.where(fieldName).gt(rhs.value)
         FormulaOperator.GreaterThanOrEqual -> Criteria.where(fieldName).gte(rhs.value)
         else -> error("$operator is not yet supported in SQL clauses")
      }
      return condition
   }

   private fun getSingleField(sourceType: ObjectType, fieldType: Type): FieldReference {
      val references = sourceType.fieldReferencesAssignableTo(fieldType)
      return when {
         references.isEmpty() -> error("Field ${fieldType.qualifiedName} is not present on type ${sourceType.qualifiedName}")
         references.size == 1 -> {
            val reference = references.single()
            if (reference.path.size == 1) {
               reference
            } else {
               error(
                  "${fieldType.qualifiedName} is only accessible on ${sourceType} via a nested property (${
                     reference.path.joinToString(
                        "."
                     )
                  }), which is not supported in SQL statements"
               )
            }
         }

         else -> error("Field ${fieldType.qualifiedName} is ambiguous on type ${sourceType.qualifiedName} - expected a single match, but found ${references.joinToString { it.description }}")
      }
   }

}
