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

data class NestedAttributeConstraint(val fieldName: String, val constraint: InputConstraint, val schema: Schema) : InputConstraint {
   override fun evaluate(argumentType: Type, value: TypedInstance): ConstraintEvaluation {
      if (value !is TypedObject) throw IllegalArgumentException("NestedAttributeConstraint must be evaluated against a TypedObject")
      val nestedAttribute = value.get(fieldName)
      val nestedTypeRef = argumentType.attributes[fieldName]!!
      val nestedType = schema.type(nestedTypeRef)
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
data class AttributeValueFromParameterConstraint(val fieldName: String, val parameterName: String) : OutputConstraint

data class ReturnValueDerivedFromParameterConstraint(val paramName: String) : OutputConstraint

data class DefaultConstraintEvaluation(override val evaluatedValue: TypedInstance, override val violation: ConstraintViolation? = null) : ConstraintEvaluation

interface ConstraintEvaluation {
   val evaluatedValue: TypedInstance
   val violation: ConstraintViolation?

   companion object {
      fun valid(evaluatedValue: TypedInstance) = DefaultConstraintEvaluation(evaluatedValue)
   }

   val isValid
      get() = this.violation == null
}

data class ConstraintEvaluations(val evaluatedValue: TypedInstance, val evaluations: List<ConstraintEvaluation>) : List<ConstraintEvaluation> by evaluations {
   val violationCount = evaluations.count { !it.isValid }
   val isValid = violationCount == 0
}

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
   fun evaluate(argumentType: Type, value: TypedInstance): ConstraintEvaluation
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

interface MetadataTarget {
   val metadata : List<Metadata>
   fun metadata(name: String): Metadata {
      return metadata.firstOrNull { it.name.fullyQualifiedName == name } ?: throw IllegalArgumentException("$name not present within this metataa")
   }
   fun hasMetadata(name : String) : Boolean {
      return this.metadata.any { it.name.fullyQualifiedName == name }
   }
}

data class Parameter(val type: Type,
                     val name: String? = null,
                     override val metadata: List<Metadata> = emptyList(),
                     val constraints: List<InputConstraint> = emptyList()) : MetadataTarget {
   fun isNamed(name: String): Boolean {
      return this.name != null && this.name == name
   }
}

data class Operation(val name: String, val parameters: List<Parameter>,
                     val returnType: Type, override val metadata: List<Metadata> = emptyList(),
                     val contract: OperationContract = OperationContract(returnType)) : MetadataTarget{

}

data class Service(val qualifiedName: String, val operations: List<Operation>, override val metadata: List<Metadata> = emptyList()) : MetadataTarget {
   fun operation(name: String): Operation {
      return this.operations.first { it.name == name }
   }
}

