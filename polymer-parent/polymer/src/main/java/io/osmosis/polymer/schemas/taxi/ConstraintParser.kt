package io.osmosis.polymer.schemas.taxi

import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.schemas.*
import lang.taxi.services.AttributeConstantValueConstraint
import lang.taxi.services.AttributeValueFromParameterConstraint
import lang.taxi.services.ReturnValueDerivedFromParameterConstraint


typealias TaxiConstraint = lang.taxi.services.Constraint

interface ConstraintProvider<out T : Constraint> {
   fun applies(constraint: TaxiConstraint): Boolean
   fun build(constrainedType: Type, constraint: TaxiConstraint, schema: Schema): T
}

interface InputConstraintProvider : ConstraintProvider<InputConstraint>
interface ContractConstraintProvider : ConstraintProvider<OutputConstraint>


class TaxiConstraintConverter(val schema: Schema) {
   private val constraintProviders = listOf(
      AttributeConstantConstraintProvider(),
      AttributeValueFromParameterConstraintProvider(),
      ReturnValueDerivedFromParameterConstraintProvider()
   )

   fun buildConstraints(type: Type, constraint: TaxiConstraint): InputConstraint {
      return buildConstraints(type, listOf(constraint)).first()
   }

   fun buildConstraints(type: Type, source: List<TaxiConstraint>): List<InputConstraint> {
      return source
         .map { buildConstraint(type, it) }
         .filterIsInstance(InputConstraint::class.java)
   }

   fun buildContract(returnType: Type, source: List<TaxiConstraint>): OperationContract {
      val constraints = source
         .map { buildConstraint(returnType, it) }
         .filterIsInstance(OutputConstraint::class.java)
      return OperationContract(returnType, constraints)
   }

   private fun buildConstraint(type: Type, constraint: TaxiConstraint): Constraint {
      return constraintProviders
         .first { it.applies(constraint) }
         .build(type, constraint, schema)

   }
}


class AttributeConstantConstraintProvider : InputConstraintProvider {
   override fun applies(constraint: TaxiConstraint): Boolean {
      return constraint is AttributeConstantValueConstraint
   }

   override fun build(constrainedType: Type, constraint: TaxiConstraint, schema: Schema): InputConstraint {
      val taxiConstraint = constraint as AttributeConstantValueConstraint
      val expectedValue = buildExpectedValueInstance(constrainedType, constraint, schema)

      return io.osmosis.polymer.schemas.AttributeConstantValueConstraint(
         taxiConstraint.fieldName,
         expectedValue
      )
   }

   private fun buildExpectedValueInstance(constrainedType: Type, constraint: AttributeConstantValueConstraint, schema: Schema): TypedInstance {
      val constrainedAttribute =
         // Note: This is techncially not possible at the moment. (it;s never null)
         // But, support for constraints on primitives is coming, so leaving this here for now,
         // since I wrote it anyway
         if (constraint.fieldName != null) {
            val typeRef = constrainedType.attributes[constraint.fieldName!!]!!
            schema.type(typeRef.name)
         } else {
            constrainedType
         }
      return TypedInstance.from(constrainedAttribute, constraint.expectedValue, schema)
   }
}

class AttributeValueFromParameterConstraintProvider : ContractConstraintProvider {
   override fun applies(constraint: TaxiConstraint): Boolean {
      return constraint is AttributeValueFromParameterConstraint
   }

   override fun build(constrainedType: Type, constraint: TaxiConstraint, schema: Schema): OutputConstraint {
      val taxiConstraint = constraint as AttributeValueFromParameterConstraint
      return io.osmosis.polymer.schemas.AttributeValueFromParameterConstraint(
         taxiConstraint.fieldName, taxiConstraint.parameterName
      )
   }
}

class ReturnValueDerivedFromParameterConstraintProvider : ContractConstraintProvider {
   override fun applies(constraint: TaxiConstraint): Boolean {
      return constraint is ReturnValueDerivedFromParameterConstraint
   }

   override fun build(constrainedType: Type, constraint: TaxiConstraint, schema: Schema): OutputConstraint {
      val taxiConstraint = constraint as ReturnValueDerivedFromParameterConstraint
      return io.osmosis.polymer.schemas.ReturnValueDerivedFromParameterConstraint(taxiConstraint.parameterName)
   }

}
