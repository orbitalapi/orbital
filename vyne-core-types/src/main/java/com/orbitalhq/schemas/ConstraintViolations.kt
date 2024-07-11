package com.orbitalhq.schemas

import arrow.core.sequence
import com.orbitalhq.models.DefinedInSchema
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import lang.taxi.expressions.Expression
import lang.taxi.expressions.LiteralExpression
import lang.taxi.expressions.OperatorExpression
import lang.taxi.expressions.TypeExpression
import lang.taxi.services.operations.constraints.*
import lang.taxi.types.ObjectType

object ReplaceValueUpdater : ConstraintViolationValueUpdater {
   override fun resolveWithUpdatedValue(updatedValue: TypedInstance): TypedInstance = updatedValue
}

class ReplaceFieldValueUpdater(private val originalValue: TypedInstance, val identifier: PropertyIdentifier) :
   ConstraintViolationValueUpdater {
   override fun resolveWithUpdatedValue(updatedValue: TypedInstance): TypedInstance {
      assert(originalValue is TypedObject, { "Can't replace field within a scalar value: $originalValue" })

      // This needs a test.
      // The idea here is that sometimes we get responses that contain replacement attributes,
      // (eg., pluck foo from the response, and copy it to foo of the original.
      //
      // Sometimes the replacements are wholesale replacements, and copying just the requested
      // field isn't enough.
      // For now, I'm assuming if the originalValue and updatedValue are the same type, then it's
      // a wholesale replacement.
      // However, that feels coincidental, there's a whole bunch of scenarios where you asked valueA
      // to be updated, and in doing so, valueB is also different.  (eg., currency conversion).
      return if (originalValue.type.name == updatedValue.type.name) {
         updatedValue
      } else {
         TODO()
//         (originalValue as TypedObject).copy(replacingArgs = mapOf(fieldName to updatedValue))
      }
   }

}

data class ConstraintExpressionEvaluationFailed(
   private val evaluatedInstance: TypedInstance,
   private val requiredType: Type,
   override val updater: ConstraintViolationValueUpdater,
   private val expression: Expression,
   private val evaluationResult: TypedInstance,
   private val schema: Schema
) : ConstraintViolation {
   /**
    * Looks at the constraint violation, and compares it against the provided operation.
    * If the contract of the operation indicates it can resolve the violation, then
    * returns a ResolutionAdvice containing the values to pass to the operation
    * to resolve the constraint.
    *
    * Note - this implementation is ported from the legacy ExpectedConstantValueMismatch
    * class. However, it only really provides resolution in specific circumstances - where
    *  - The operation return value refers to an input param
    *  - The input param has the same type as a type expression found in the violated expression.
    *
    *  In practice, this code hasn't been used in production, so the
    *  goal here in rewriting is to make existing tests pass.
    */
   override fun provideResolutionAdvice(operation: Operation, contract: OperationContract): ResolutionAdvice? {
      return if (
         operation.returnType.isAssignableTo(requiredType)
         && contract.containsConstraint(ReturnValueDerivedFromParameterConstraint::class.java)
      ) {
         val constraintViolatingParam =
            PropertyFieldNameIdentifier(contract.constraint(ReturnValueDerivedFromParameterConstraint::class.java).attributePath) to evaluatedInstance

         val paramToAdjust = findParamReferredToInExpression(expression, operation.parameters)
            ?: return null

         val paramValue = findRequiredValueDefinedInExpression(expression, paramToAdjust.type, schema)
            ?: return null

         require(paramToAdjust.name != null) { "Constraints must be defined against parameters with names - but contract of ${operation.name} refers to parameter without a name" }
         val paramName = PropertyFieldNameIdentifier(paramToAdjust.name)
         val paramToAdjustViolatingField = paramName to paramValue
         ResolutionAdvice(operation, listOf(constraintViolatingParam, paramToAdjustViolatingField).toMap())
      } else {
         null
      }
   }

   /**
    * Looks at the parameters provided for the operation, and the parts of the expression,
    * and returns the parameter that is referred to within the expression
    */
   private fun findParamReferredToInExpression(expression: Expression, parameters: List<Parameter>): Parameter? {
      if (expression !is OperatorExpression) {
         return null
      }
      val expressionParts = setOf(expression.lhs, expression.rhs)
      return expressionParts
         .sequence()!!
         .mapNotNull { expressionPart ->
            when (expressionPart) {
               is TypeExpression -> parameters.firstOrNull { it.type.name.parameterizedName == expressionPart.returnType.toQualifiedName().parameterizedName }
               else -> null
            }
         }
         .firstOrNull()
   }

   private fun findRequiredValueDefinedInExpression(
      expression: Expression,
      requiredType: Type,
      schema: Schema
   ): TypedInstance? {
      if (expression !is OperatorExpression) {
         return null
      }
      val expressionParts = setOf(expression.lhs, expression.rhs)
      val value = expressionParts.filterIsInstance<LiteralExpression>()
         .singleOrNull()
         ?.value
         ?: return null

      return TypedInstance.from(requiredType, value, schema, source = DefinedInSchema)
   }
}
