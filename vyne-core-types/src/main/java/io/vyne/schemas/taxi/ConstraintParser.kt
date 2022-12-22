package io.vyne.schemas.taxi

import io.vyne.schemas.*
import lang.taxi.query.convertToPropertyConstraint
import lang.taxi.services.operations.constraints.PropertyToParameterConstraint
import lang.taxi.services.operations.constraints.ReturnValueDerivedFromParameterConstraint


class FunctionConstraintProvider(val function: () -> List<InputConstraint>) : DeferredConstraintProvider {
   override fun buildConstraints(): List<InputConstraint> = function.invoke()
}

class TaxiConstraintConverter(val schema: Schema) {
   private val constraintProviders = listOf(
//      AttributeConstantConstraintProvider(),
//      AttributeValueFromParameterConstraintProvider(),
      PropertyToParameterConstraintProvider(),
      ReturnValueDerivedFromParameterConstraintProvider(),
      ExpressionConstraintProvider()
   )

   fun buildConstraints(type: Type, constraint: TaxiConstraint): InputConstraint {
      return buildConstraints(type, listOf(constraint)).first()
   }

   fun buildConstraints(type: Type, source: List<TaxiConstraint>): List<InputConstraint> {
      // TODO: Right now, only considering nested constraints on parameter types.
      // This may be invalid.
      // The reason is two-fold:
      // A) Do we always want to recurse into every parameter type and check for constraints?
      // is this too heavy?  (Is it a premature optimisation not to?)
      // B) If we DO find a violated constraint somewhere on a non-parameter type, what
      // are we going to do about it?  We can't resolve it, as we shouldn't mutate non-parameter types.
      // Having written all that, I'm almost certain this is wrong.  But, it's what I'm doing now.
      // FIXME later.
      val nestedConstraints = if (type.isParameterType) {
         type.attributes.flatMap { (attributeName, field) ->
            field.constraints
               .filterIsInstance(InputConstraint::class.java)
               .map { NestedAttributeConstraint(attributeName, it, schema) }
         }
      } else emptyList()

      val constraints = source
         .flatMap { buildConstraint(type, it) }
         .plus(nestedConstraints)
      return constraints
         .filterIsInstance(InputConstraint::class.java)


   }

   fun buildOutputConstraints(type: Type, source: List<TaxiConstraint>): List<OutputConstraint> {
      return source
         .flatMap { buildConstraint(type, it) }
         .filterIsInstance(OutputConstraint::class.java)
   }

   fun buildContract(returnType: Type, source: List<TaxiConstraint>): OperationContract {
      val constraints = buildOutputConstraints(returnType, source)
      return OperationContract(returnType, constraints)
   }

   private fun buildConstraint(type: Type, constraint: TaxiConstraint): List<Constraint> {
      return constraintProviders
         .first { it.applies(constraint) }
         .build(type, constraint, schema)

   }
}

class ExpressionConstraintProvider : ContractConstraintProvider {
   private val constraintProvider = PropertyToParameterConstraintProvider()
   override fun applies(constraint: TaxiConstraint): Boolean {
      return constraint is lang.taxi.services.operations.constraints.ExpressionConstraint
   }

   override fun build(constrainedType: Type, constraint: TaxiConstraint, schema: Schema): List<OutputConstraint> {
      val taxiConstraint = (constraint as lang.taxi.services.operations.constraints.ExpressionConstraint)
      val propertyConstraints = taxiConstraint.convertToPropertyConstraint()
      return propertyConstraints.flatMap { propertyConstraint ->
         constraintProvider.build(constrainedType, propertyConstraint, schema)
      }
   }
}


class PropertyToParameterConstraintProvider : ContractConstraintProvider {
   override fun applies(constraint: TaxiConstraint): Boolean {
      return constraint is PropertyToParameterConstraint
   }

   override fun build(constrainedType: Type, constraint: TaxiConstraint, schema: Schema): List<OutputConstraint> {
      val taxiConstraint = constraint as PropertyToParameterConstraint
      return listOf(
         PropertyToParameterConstraint(
            taxiConstraint.propertyIdentifier,
            taxiConstraint.operator,
            taxiConstraint.expectedValue
         )
      )
   }
}

class ReturnValueDerivedFromParameterConstraintProvider : ContractConstraintProvider {
   override fun applies(constraint: TaxiConstraint): Boolean {
      return constraint is ReturnValueDerivedFromParameterConstraint
   }

   override fun build(constrainedType: Type, constraint: TaxiConstraint, schema: Schema): List<OutputConstraint> {
      val taxiConstraint = constraint as ReturnValueDerivedFromParameterConstraint
      return listOf(ReturnValueDerivedFromParameterConstraint(taxiConstraint.attributePath))
   }

}
