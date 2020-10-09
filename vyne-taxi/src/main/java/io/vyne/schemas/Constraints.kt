package io.vyne.schemas

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vyne.models.DefinedInSchema
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.models.TypedValue
import lang.taxi.Operator
import lang.taxi.services.operations.constraints.*
import lang.taxi.types.AttributePath
import lang.taxi.types.EnumValue
import lang.taxi.types.ValueExpression

/**
 * Indicates that an attribute of a parameter (which is an Object type)
 * must have a constant value
 * eg:
 * Given Money(amount:Decimal, currency:String),
 * could express that Money.currency must have a value of 'GBP'
 */
data class AttributeConstantValueConstraint(val fieldName: String, val expectedValue: TypedInstance) : InputConstraint {
   override fun evaluate(argumentType: Type, value: TypedInstance, schema: Schema): ConstraintEvaluation {
      fun evaluationResult(actualValue: TypedInstance, updater: ConstraintViolationValueUpdater): ConstraintEvaluation {
         if (expectedValue == actualValue) return ConstraintEvaluation.valid(value)

         // TODO : This feels wrong.  Why pass type+field, when the field itself is supposed to be self-describing.
         // But, how do we navigate from an attribute to it's parent.
         // Eg: from Money.currency -> Money
         TODO()
//         return DefaultConstraintEvaluation(value, ExpectedConstantValueMismatch(value, argumentType, fieldName, expectedValue, actualValue, updater))
      }
      when (value) {
//         is TypedObject -> return evaluationResult(value[fieldName], ReplaceFieldValueUpdater(value, fieldName))
//         is TypedValue -> return evaluationResult(value, ReplaceValueUpdater)
         else -> error("not supported on type ${value::class.java} ")
      }
   }
}

data class NestedAttributeConstraint(val fieldName: String, val constraint: InputConstraint, @JsonIgnore val schema: Schema) : InputConstraint {
   override fun evaluate(argumentType: Type, value: TypedInstance, schema: Schema): ConstraintEvaluation {
      if (value !is TypedObject) throw IllegalArgumentException("NestedAttributeConstraint must be evaluated against a TypedObject")
      val nestedAttribute = value.get(fieldName)
      val field = argumentType.attributes.getValue(fieldName)
      val nestedType = schema.type(field.type)
      // This is probably wrong - find the argument type of the nested field
      return NestedConstraintEvaluation(value, fieldName, constraint.evaluate(nestedType, nestedAttribute, schema))
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

data class ReturnValueDerivedFromParameterConstraint(val propertyIdentifier: PropertyIdentifier) : OutputConstraint {
   constructor(attributePath: AttributePath) : this(PropertyFieldNameIdentifier(attributePath))
}

class PropertyToParameterConstraint(propertyIdentifier: PropertyIdentifier,
                                    operator: Operator = Operator.EQUAL,
                                    expectedValue: ValueExpression) : lang.taxi.services.operations.constraints.PropertyToParameterConstraint(
   propertyIdentifier, operator, expectedValue, emptyList()
), OutputConstraint, InputConstraint {
   override fun evaluate(argumentType: Type, value: TypedInstance, schema: Schema): ConstraintEvaluation {
      val resolvedActualValue = when (value) {
         is TypedObject -> value.getAttribute(propertyIdentifier, schema)
         is TypedValue -> value // TODO : we should be evaluating the path here, or validating, or...something.
         // TODO : Why isn't the default here to just use the value passed?
         else -> error("cannot determine actual value  on type ${value::class.java}")
      }


      val resolvedExpectedValue = resolveExpectedValue(argumentType, value, schema)

      fun evaluationResult(actualValue: TypedInstance, updater: ConstraintViolationValueUpdater): ConstraintEvaluation {
         if (resolvedExpectedValue == actualValue) return ConstraintEvaluation.valid(value)

         // TODO : This feels wrong.  Why pass type+field, when the field itself is supposed to be self-describing.
         // But, how do we navigate from an attribute to it's parent.
         // Eg: from Money.currency -> Money
         return DefaultConstraintEvaluation(value, ExpectedConstantValueMismatch(value, argumentType, propertyIdentifier, resolvedExpectedValue, actualValue, updater))
      }
      return when (value) {
         is TypedObject -> evaluationResult(resolvedActualValue, ReplaceFieldValueUpdater(value, propertyIdentifier))
         is TypedValue -> evaluationResult(value, ReplaceValueUpdater)
         else -> error("not supported on type ${value::class.java} ")
      }
      TODO("Not yet implemented")
   }

   private fun resolveExpectedValue(argumentType: Type, value: TypedInstance, schema: Schema): TypedInstance {
      return when (expectedValue) {
         is ConstantValueExpression -> {
            // We expected a constant value.  Convert the value we were given into the appropriate type
            val type = schema.type(argumentType.attribute(propertyIdentifier).type)
            TypedInstance.from(type, expectedValue.value, schema, source = DefinedInSchema)
         }
         // Note : Added this for completeness to make the compiler happy.
         // If the below EnumValueExpression case causes problems, lets change it -- not much thought went into this.
         is EnumValueExpression -> {
            val (typeName,enumName) = EnumValue.qualifiedNameFrom(expectedValue.enumValue.qualifiedName)
            val enumType = schema.type(typeName.fullyQualifiedName)
            TypedInstance.from(enumType, enumName, schema, source = DefinedInSchema)
         }
         is RelativeValueExpression -> {
            when (value) {
               // This seems wrong - we shouldn't be accessing the the expected value path against value,
               // as it might be constrainted against something else.
               // Lets see if the tests pass...
               is TypedObject -> value[expectedValue.path]
               else -> error("Relative value expressions are not supported on values of type ${value::class.simpleName}")
            }
         }
         else -> {
            error("Unexpected value expression type:  ${expectedValue::class.simpleName}")
         }
      }
   }

}

