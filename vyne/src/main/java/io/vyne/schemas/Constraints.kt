package io.vyne.schemas

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.models.TypedValue
import lang.taxi.types.AttributePath

/**
* Indicates that an attribute of a parameter (which is an Object type)
* must have a constant value
* eg:
* Given Money(amount:Decimal, currency:String),
* could express that Money.currency must have a value of 'GBP'
*/
data class AttributeConstantValueConstraint(val fieldName: String, val expectedValue: TypedInstance) : InputConstraint {
   override fun evaluate(argumentType: Type, value: TypedInstance): ConstraintEvaluation {
      fun evaluationResult(actualValue: TypedInstance, updater: ConstraintViolationValueUpdater): ConstraintEvaluation {
         if (expectedValue == actualValue) return ConstraintEvaluation.valid(value)

         // TODO : This feels wrong.  Why pass type+field, when the field itself is supposed to be self-describing.
         // But, how do we navigate from an attribute to it's parent.
         // Eg: from Money.currency -> Money
         return DefaultConstraintEvaluation(value, ExpectedConstantValueMismatch(value, argumentType, fieldName, expectedValue, actualValue, updater))
      }
      when (value) {
         is TypedObject -> return evaluationResult(value[fieldName], ReplaceFieldValueUpdater(value, fieldName))
         is TypedValue -> return evaluationResult(value, ReplaceValueUpdater)
         else -> error("not supported on type ${value::class.java} ")
      }
   }
}

data class NestedAttributeConstraint(val fieldName: String, val constraint: InputConstraint, @JsonIgnore val schema: Schema) : InputConstraint {
   override fun evaluate(argumentType: Type, value: TypedInstance): ConstraintEvaluation {
      if (value !is TypedObject) throw IllegalArgumentException("NestedAttributeConstraint must be evaluated against a TypedObject")
      val nestedAttribute = value.get(fieldName)
      val field = argumentType.attributes.getValue(fieldName)
      val nestedType = schema.type(field.type)
      // This is probably wrong - find the argument type of the nested field
      return NestedConstraintEvaluation(value, fieldName, constraint.evaluate(nestedType, nestedAttribute))
   }
}

data class NestedConstraintEvaluation(val parent: TypedObject, val fieldName: String, private val evaluation: ConstraintEvaluation) : ConstraintEvaluation {
   override val evaluatedValue: TypedInstance = evaluation.evaluatedValue
   override val violation: ConstraintViolation?
      get() {
         if (evaluation.violation == null) return null
         return NestedConstraintViolation(evaluation.violation!!, this)
      }
}

class NestedConstraintViolation(violation: ConstraintViolation, private val evaluation: NestedConstraintEvaluation) : ConstraintViolation by violation {
   override fun resolveWithUpdatedValue(updatedValue: TypedInstance): TypedInstance {
      val resolvedParent = evaluation.parent.copy(mapOf(evaluation.fieldName to updatedValue))
      return resolvedParent
   }
}

/**
 * Indicates that an attribute will be returned updated to a value
 * provided by a parameter (ie., an input on a function)
 */
data class AttributeValueFromParameterConstraint(val fieldName: String, val attributePath: AttributePath) : OutputConstraint

data class ReturnValueDerivedFromParameterConstraint(val attributePath: AttributePath) : OutputConstraint

