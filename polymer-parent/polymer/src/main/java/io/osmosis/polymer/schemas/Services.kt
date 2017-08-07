package io.osmosis.polymer.schemas

import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.models.TypedObject
import io.osmosis.polymer.models.TypedValue

/**
 * DESIGN NOTE:
 * I've duplicated these between Taxi and Polymer, to allow the two projects to remain
 * independent.
 * There idea is that Polymer shouldn't have a core-level coupling to Taxi.
 * As a result, there's high duplication here.  Revisit this decision later if this
 * starts to suck.
 *
 * Also, while at the time of writing, there's high duplication, I expect
 * that polymer may add more 'constraint evaluation' features, which aren't
 * meaningful within Taxi.
 */

/**
 * Indicates that an attribute of a parameter (which is an Object type)
 * must have a constant value
 * eg:
 * Given Money(amount:Decimal, currency:String),
 * could express that Money.currency must have a value of 'GBP'
 */
data class AttributeConstantValueConstraint(val fieldName: String, val expectedValue: TypedInstance) : InputConstraint {
   override fun evaluate(param: Parameter, value: TypedInstance): ConstraintEvaluation {
      fun evaluationResult(actualValue: TypedInstance): ConstraintEvaluation {
         if (expectedValue == actualValue) return ConstraintEvaluation.valid(value)

         // TODO : This feels wrong.  Why pass type+field, when the field itself is supposed to be self-describing.
         // But, how do we navigate from an attribute to it's parent.
         // Eg: from Money.currency -> Money
         return ConstraintEvaluation(value, ExpectedConstantValueMismatch(value, param.type, fieldName, expectedValue, actualValue))
      }
      when (value) {
         is TypedObject -> return evaluationResult(value[fieldName]!!)
         is TypedValue -> return evaluationResult(value)
         else -> error("not supported on type ${value::class.java} ")
      }
   }
}

/**
 * Indicates that an attribute will be returned updated to a value
 * provided by a parameter (ie., an input on a function)
 */
data class AttributeValueFromParameterConstraint(val fieldName: String, val parameterName: String) : OutputConstraint

data class ReturnValueDerivedFromParameterConstraint(val paramName: String) : OutputConstraint
data class ConstraintEvaluation(val evaluatedValue: TypedInstance, val violation: ConstraintViolation? = null) {
   companion object {
      fun valid(evaluatedValue: TypedInstance) = ConstraintEvaluation(evaluatedValue)
   }

   val isValid = violation == null
}

data class ConstraintEvaluations(val evaluatedValue: TypedInstance,val evaluations: List<ConstraintEvaluation>) : List<ConstraintEvaluation> by evaluations {
   val violationCount = evaluations.count { !it.isValid }
   val isValid = violationCount == 0
}

interface InputConstraint : Constraint {
   fun evaluate(param: Parameter, value: TypedInstance): ConstraintEvaluation
}

data class OperationContract(val returnType: Type, val constraints: List<OutputConstraint> = emptyList()) {
   fun containsConstraint(clazz: Class<out OutputConstraint>): Boolean {
      return constraints.filterIsInstance(clazz)
         .isNotEmpty()
   }

   fun <T : OutputConstraint> containsConstraint(clazz: Class<T>, predicate: (T) -> Boolean): Boolean {
      return constraints.filterIsInstance(clazz)
         .filter(predicate)
         .isNotEmpty()
   }

   fun <T : OutputConstraint> constraint(clazz: Class<T>, predicate: (T) -> Boolean): T {
      return constraints.filterIsInstance(clazz)
         .filter(predicate)
         .first()
   }

   fun <T : OutputConstraint> constraint(clazz: Class<T>): T {
      return constraints.filterIsInstance(clazz).first()
   }
}

interface OutputConstraint : Constraint

interface Constraint

data class Parameter(val type: Type,
                     val name: String? = null,
                     val metadata: List<Metadata> = emptyList(),
                     val constraints: List<InputConstraint> = emptyList()) {
   fun isNamed(name: String): Boolean {
      return this.name != null && this.name == name
   }
}

data class Operation(val name: String, val parameters: List<Parameter>,
                     val returnType: Type, val metadata: List<Metadata> = emptyList(),
                     val contract: OperationContract = OperationContract(returnType)) {
   fun metadata(name: String): Metadata {
      return metadata.firstOrNull { it.name.fullyQualifiedName == name } ?: throw IllegalArgumentException("$name not present within this metataa")
   }
}

data class Service(val qualifiedName: String, val operations: List<Operation>, val metadata: List<Metadata> = emptyList()) {
   fun operation(name: String): Operation {
      return this.operations.first { it.name == name }
   }
}

