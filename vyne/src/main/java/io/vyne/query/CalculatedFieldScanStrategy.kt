package io.vyne.query

import io.vyne.models.Calculated
import io.vyne.models.TypedInstance
import io.vyne.query.formulas.CalculatorRegistry
import io.vyne.schemas.Type

class CalculatedFieldScanStrategy(private val calculatorRegistry: CalculatorRegistry): QueryStrategy {
   override fun invoke(target: Set<QuerySpecTypeNode>, context: QueryContext): QueryStrategyResult {
      if (context.debugProfiling) {// enable profiling via context.debugProfiling=true flag
         return context.startChild(this, "scan for matches", OperationType.LOOKUP) { operation ->
            scanForMatches(target, context)
         }
      }
      return scanForMatches(target, context)
   }

   private fun scanForMatches(target: Set<QuerySpecTypeNode>, context: QueryContext): QueryStrategyResult {
      val targetTypes: Map<Type, QuerySpecTypeNode> = target
         .filter { it.type.isCalculated }
         .associateBy { it.type }

      val matches = targetTypes
         .map { (type, querySpec) -> querySpec to tryCalculate(type, context, querySpec.mode.discoveryStrategy()) }
         .filter { it.second != null }
         .toMap()
      return QueryStrategyResult(matches)
   }

   fun tryCalculate(calculatedType: Type, context: QueryContext, factDiscoveryStrategy: FactDiscoveryStrategy): TypedInstance? {
      val calculation = calculatedType.calculation!!
      val operands =  calculation.operandFields
      val operandValues = operands.map { operand ->
         val operandType = context.schema.type(operand.fullyQualifiedName)
         context.getFactOrNull(operandType, factDiscoveryStrategy)?.value
      }

      return if (operandValues.any { it == null }) {
         null
      } else {
        TypedInstance.from(
           type = calculatedType,
           value = calculatorRegistry.calculate(calculation.operator, operandValues as List<Any>),
           schema =  context.schema,
           source =  Calculated)
      }
   }
}
