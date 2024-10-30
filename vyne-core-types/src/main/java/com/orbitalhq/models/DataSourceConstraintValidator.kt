package com.orbitalhq.models

import com.orbitalhq.schemas.QueryOperation
import com.orbitalhq.schemas.Schema
import lang.taxi.services.operations.constraints.Constraint

class DataSourceConstraintValidator {
   fun sourceSatisfiesConstraint(constraints: List<Constraint>, value: TypedInstance, schema: Schema): Boolean {
      return when (val source: DataSource = value.source) {
         is OperationResultReference -> operationSatisfiesContract(source, constraints, schema)

         // The data wasn't loaded from an operation.
         // We can't be sure it satisfies the contract, even if we look at the
         // data, so we have to return false
         else -> false
      }
   }

   private fun operationSatisfiesContract(
      source: OperationResultReference,
      constraints: List<Constraint>,
      schema: Schema
   ): Boolean {
      val (_,operation) = schema.remoteOperation(source.operationName)

      // In future, we can be smarter about this
      // But queryOperations are talking to databases, so capable of satifying contracts.
      if (operation is QueryOperation) {
         return true
      }
      val operationSatisfiesAllConstraints = constraints.map { requiredConstraint ->
         operation.contract.satisfies(requiredConstraint)
      }.all { it.satisfiesRequestedConstraint }
      return operationSatisfiesAllConstraints
   }
}
