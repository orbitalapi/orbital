package io.vyne.query.policyManager

import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.schemas.Operation
import lang.taxi.policies.Instruction

interface InstructionProcessor {
   val instructionType: Instruction.InstructionType
   fun process(instruction: Instruction, value: TypedInstance): TypedInstance
}

object InstructionProcessors {
   private val processors = listOf(
      PermitInstructionProcessor(),
      FilterInstructionProcessor()
   ).associateBy { it.instructionType }

   fun get(instruction: Instruction): InstructionProcessor {
      return processors[instruction.type] ?: error("No processor defined for instruction $instruction")
   }
}

class PermitInstructionProcessor : InstructionProcessor {
   override val instructionType = Instruction.InstructionType.PERMIT
   override fun process(instruction: Instruction, value: TypedInstance): TypedInstance {
      return value
   }
}

class FilterInstructionProcessor : InstructionProcessor {
   override fun process(instruction: Instruction, value: TypedInstance): TypedInstance {
      return TypedNull(value.type)
   }

   override val instructionType = Instruction.InstructionType.FILTER

}
