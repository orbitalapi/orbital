package com.orbitalhq.connectors.hazelcast.invoker

import com.hazelcast.query.Predicate
import com.hazelcast.query.Predicates
import com.orbitalhq.connectors.utils.getSingleField
import lang.taxi.expressions.Expression
import lang.taxi.expressions.LiteralExpression
import lang.taxi.expressions.OperatorExpression
import lang.taxi.expressions.TypeExpression
import lang.taxi.query.DiscoveryType
import lang.taxi.types.Arrays
import lang.taxi.types.FormulaOperator
import lang.taxi.types.ObjectType

object ExpressionToPredicateConverter {
   fun convert(discoveryType: DiscoveryType, expression: OperatorExpression): Predicate<Any, Any> {
      discoveryType.type
      val expressionParts = setOf(expression.lhs, expression.rhs)
      val predicate = when {
         expression.lhs is OperatorExpression && expression.rhs is OperatorExpression -> buildCompoundPredicate(
            discoveryType,
            expression
         )

         expressionParts.containsType(LiteralExpression::class.java) && expressionParts.containsType(TypeExpression::class.java) -> buildComparisonPredicate(
            discoveryType,
            expression,
            expressionParts
         )

         else -> error("Predicate construction is not supported for statements containing types ${expressionParts.joinToString { it::class.java.simpleName }}")
      }
      return predicate
   }

   private fun buildComparisonPredicate(
      discoveryType: DiscoveryType,
      expression: OperatorExpression,
      expressionParts: Set<Expression>
   ): Predicate<Any, Any> {
      val sourceType = Arrays.unwrapPossibleArrayType(discoveryType.type)
      require(sourceType is ObjectType) { "Predicates are currently only buildable against Object types (or collections of Object types). Found type ${sourceType.qualifiedName}" }

      val comparisonValueExpression = expressionParts.filterIsInstance<LiteralExpression>().single()
      val comparisonValue = comparisonValueExpression.literal.value
      // This shouldn't happen.
      require(comparisonValue is Comparable<*>) { "Internal error: Hazelcast requires all primitives are comparable. Found that type ${comparisonValue::class.simpleName} does not implement Comparable" }

      val comparisonType = expressionParts.filterIsInstance<TypeExpression>()
         .single()
         .type
      val fieldReference = getSingleField(sourceType, comparisonType)
      require(fieldReference.path.size == 1) { "Expected a direct (not nested field), but searching for type ${comparisonType.qualifiedName} against ${sourceType.qualifiedName} returned a path of ${fieldReference.description}" }

      val fieldName = fieldReference.path.single().name
      return when (expression.operator) {
         FormulaOperator.Equal -> Predicates.equal(fieldName, comparisonValue)
         FormulaOperator.NotEqual -> Predicates.notEqual(fieldName, comparisonValue)
         FormulaOperator.LessThan -> Predicates.lessThan(fieldName, comparisonValue)
         FormulaOperator.LessThanOrEqual -> Predicates.lessEqual(fieldName, comparisonValue)
         FormulaOperator.GreaterThan -> Predicates.greaterThan(fieldName, comparisonValue)
         FormulaOperator.GreaterThanOrEqual -> Predicates.greaterEqual(fieldName, comparisonValue)
         else -> error("Comparison expressions are not supported with operator ${expression.operator.name}")
      }
   }

   private fun buildCompoundPredicate(
      discoveryType: DiscoveryType,
      expression: OperatorExpression
   ): Predicate<Any, Any> {
      require(expression.lhs is OperatorExpression) { "Only OperatorExpressions are supported in Hazelcast expression builder, found ${expression.lhs::class.simpleName}" }
      require(expression.rhs is OperatorExpression) { "Only OperatorExpressions are supported in Hazelcast expression builder, found ${expression.rhs::class.simpleName}" }

      val lhs = convert(discoveryType, expression.lhs as OperatorExpression)
      val rhs = convert(discoveryType, expression.rhs as OperatorExpression)

      val predicate = when (expression.operator) {
         FormulaOperator.LogicalAnd -> Predicates.and<Any, Any>(lhs, rhs)
         FormulaOperator.LogicalOr -> Predicates.or(lhs, rhs)
         else -> error("Compound expressions are not supported with an operator type of ${expression.operator.name}")
      }
      return predicate

   }
}

fun <R> Set<*>.containsType(type: Class<R>): Boolean {
   return this.filterIsInstance(type).isNotEmpty()
}
