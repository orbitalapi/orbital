package io.vyne.schemas

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vyne.models.DefinedInSchema
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.models.TypedValue
import lang.taxi.Operator
import lang.taxi.expressions.Expression
import lang.taxi.services.operations.constraints.ConstantValueExpression
import lang.taxi.services.operations.constraints.PropertyFieldNameIdentifier
import lang.taxi.services.operations.constraints.PropertyIdentifier
import lang.taxi.services.operations.constraints.RelativeValueExpression
import lang.taxi.services.operations.constraints.ValueExpression
import lang.taxi.types.AttributePath

typealias TaxiConstraint = lang.taxi.services.operations.constraints.Constraint

interface ConstraintEvaluation {
   val evaluatedValue: TypedInstance
   val violation: ConstraintViolation?

   companion object {
      fun valid(evaluatedValue: TypedInstance) = DefaultConstraintEvaluation(evaluatedValue)
   }

   val isValid
      get() = this.violation == null
}

data class DefaultConstraintEvaluation(override val evaluatedValue: TypedInstance, override val violation: ConstraintViolation? = null) : ConstraintEvaluation {
   override fun toString(): String {
      return "DefaultConstraintEvaluation - evaluatedValue: ${evaluatedValue.typeName} ${evaluatedValue.toRawObject()} , violation: $violation"
   }
}

data class ConstraintEvaluations(val evaluatedValue: TypedInstance, val evaluations: List<ConstraintEvaluation>) : List<ConstraintEvaluation> by evaluations {
   val violationCount = evaluations.count { !it.isValid }
   val isValid = violationCount == 0
}

interface Constraint

interface InputConstraint : Constraint {
   // note:
   // This USED to take param: Parameter as the first argument.
   // I've swapped it to type.
   // When evaluating constraints on nested fields (eg., parameter types)
   // using Param becomes awkward.
   // The Param is actually the parent, but the constraint is on the nested attribute
   // Therefore, swapping it out to type.
   // It's possible this may need to be richer to pass additional attributes
   // from the param wrapper, but at present, type is all we're using.
   fun evaluate(argumentType: Type, value: TypedInstance, schema: Schema): ConstraintEvaluation
}

interface OutputConstraint : Constraint
interface InputConstraintProvider : ConstraintProvider<InputConstraint>
interface ContractConstraintProvider : ConstraintProvider<OutputConstraint>

interface ConstraintProvider<out T : Constraint> {
   fun applies(constraint: TaxiConstraint): Boolean
   fun build(constrainedType: Type, constraint: TaxiConstraint, schema: Schema): List<T>
}

interface DeferredConstraintProvider {
   fun buildConstraints(): List<InputConstraint>
}

class EmptyDeferredConstraintProvider : DeferredConstraintProvider {
   override fun buildConstraints(): List<InputConstraint> = emptyList()
}


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

@Deprecated("Migrate to using ExpressionConstraint instead, which is the more generic form of this concept.  It's not implemented yet, but it's the direction of travel")
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
   }

   private fun resolveExpectedValue(argumentType: Type, value: TypedInstance, schema: Schema): TypedInstance {
      return when (expectedValue) {
         is ConstantValueExpression -> {
            // We expected a constant value.  Convert the value we were given into the appropriate type
            val type = schema.type(argumentType.attribute(propertyIdentifier).type)
            TypedInstance.from(type, (expectedValue as ConstantValueExpression).value, schema, source = DefinedInSchema)
         }
         is RelativeValueExpression -> {
            when (value) {
               // This seems wrong - we shouldn't be accessing the the expected value path against value,
               // as it might be constrainted against something else.
               // Lets see if the tests pass...
               is TypedObject -> value[(expectedValue as RelativeValueExpression).path]
               else -> error("Relative value expressions are not supported on values of type ${value::class.simpleName}")
            }
         }
      }
   }

}

data class ExpressionConstraint(private val expression: Expression): OutputConstraint, InputConstraint {
   override fun evaluate(argumentType: Type, value: TypedInstance, schema: Schema): ConstraintEvaluation {
      TODO("Not yet implemented")
   }

}

