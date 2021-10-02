package io.vyne.models.conditional

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.vyne.models.EvaluationValueSupplier
import io.vyne.schemas.Type
import lang.taxi.types.AndExpression
import lang.taxi.types.ComparisonExpression
import lang.taxi.types.ComparisonOperator
import lang.taxi.types.ConstantEntity
import lang.taxi.types.ElseMatchExpression
import lang.taxi.types.FieldReferenceEntity
import lang.taxi.types.LogicalExpression
import lang.taxi.types.OrExpression
import lang.taxi.types.WhenCaseBlock
import java.math.BigDecimal
import java.util.Stack

object LogicalExpressionEvaluator {
   fun evaluate(cases: List<WhenCaseBlock>, factory: EvaluationValueSupplier, type: Type): WhenCaseBlock? {
      return cases.firstOrNull { case ->
         when (case.matchExpression) {
            is LogicalExpression -> {
               val expressionStack = Stack<Either<LogicalExpression, LogicalOp>>()
               pushExpression(case.matchExpression as LogicalExpression, expressionStack)
               evaluateExpressionStack(expressionStack, type, factory)
            }
            is ElseMatchExpression -> true
            else -> false
         }
      }
   }

   private fun pushExpression(logicalExpression: LogicalExpression, expressionStack: Stack<Either<LogicalExpression, LogicalOp>>) {
       when (logicalExpression) {
         is ComparisonExpression -> expressionStack.push(logicalExpression.left())
         is AndExpression -> {
            pushExpression(logicalExpression.right, expressionStack)
            expressionStack.push(LogicalOp.And.right())
            pushExpression(logicalExpression.left, expressionStack)
         }
         is OrExpression -> {
            pushExpression(logicalExpression.right, expressionStack)
            expressionStack.push(LogicalOp.Or.right())
            pushExpression(logicalExpression.left, expressionStack)
         }
      }
   }

   private fun evaluateExpressionStack(expressionStack: Stack<Either<LogicalExpression, LogicalOp>>, type: Type, factory: EvaluationValueSupplier): Boolean {
      var result  = false
      var lastLogicalOp: LogicalOp? = null
      while (!expressionStack.empty()) {
         when (val item = expressionStack.pop()) {
            is Either.Left -> result = when (lastLogicalOp) {
               LogicalOp.And -> result && evaluateComparisonExpression(item.a as ComparisonExpression, factory)
               LogicalOp.Or -> result || evaluateComparisonExpression(item.a as ComparisonExpression, factory)
               else -> evaluateComparisonExpression(item.a as ComparisonExpression, factory)
            }
            is Either.Right -> lastLogicalOp = item.b
         }
      }
      return result
   }


   private fun evaluateComparisonExpression(logicalExpression: ComparisonExpression, factory: EvaluationValueSupplier): Boolean {
      val right = logicalExpression.right
      val left = logicalExpression.left
      val (leftValue, rightValue) = when {
         right is FieldReferenceEntity && left is FieldReferenceEntity -> {
            (factory.getValue(left.fieldName).value to factory.getValue(right.fieldName).value)
         }
         right is ConstantEntity && left is FieldReferenceEntity -> {
           factory.getValue(left.fieldName).value to right.value
         }

         right is FieldReferenceEntity && left is ConstantEntity -> {
            left.value to factory.getValue(right.fieldName).value
         }
         else -> null to null
      }

      return evaluateExpression(leftValue, rightValue, logicalExpression.operator)
   }

   private fun evaluateExpression(left: Any?, right: Any?, operator: ComparisonOperator): Boolean {
      return when {
         left == null && right == null && operator == ComparisonOperator.EQ -> true
         left == null && right != null && operator == ComparisonOperator.NQ -> true
         left != null && right == null && operator == ComparisonOperator.NQ -> true
         left != null && right != null -> evaluateExpressionForPopulatedOperands(left, right, operator)
         else -> false
      }
   }

   private fun evaluateExpressionForPopulatedOperands(left: Any, right: Any, operator: ComparisonOperator): Boolean {
     return  when  {
        left is Int && right is Int -> evaluateComparables(left, right, operator)
        left is BigDecimal && right is BigDecimal -> evaluateComparables(left, right, operator)
        left is String && right is String -> evaluateComparables(left, right, operator)
        else -> false
      }
   }

   private fun <T: Comparable<T>> evaluateComparables(left: T, right: T, operator: ComparisonOperator): Boolean {
      return when (operator) {
         ComparisonOperator.LT -> left < right
         ComparisonOperator.NQ -> left != right
         ComparisonOperator.EQ -> left == right
         ComparisonOperator.GE -> left >= right
         ComparisonOperator.GT -> left > right
         ComparisonOperator.LE -> left <= right
      }
   }
}

enum class LogicalOp {
   And,
   Or
}
