package io.vyne.query.policyManager

import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import lang.taxi.policies.Instruction

interface InstructionExecutor {
   val instructionType: Instruction.InstructionType
   fun execute(instruction: Instruction, value: TypedInstance): TypedInstance
}

object InstructionExecutors {
   private val executors = listOf(
      PermitInstructionExecutor(),
      FilterInstructionExecutor(),
      ProcessInstructionExecutor()
   ).associateBy { it.instructionType }

   fun get(instruction: Instruction): InstructionExecutor {
      return executors[instruction.type] ?: error("No processor defined for instruction $instruction")
   }
}

class PermitInstructionExecutor : InstructionExecutor {
   override val instructionType = Instruction.InstructionType.PERMIT
   override fun execute(instruction: Instruction, value: TypedInstance): TypedInstance {
      return value
   }
}

class FilterInstructionExecutor : InstructionExecutor {
   override fun execute(instruction: Instruction, value: TypedInstance): TypedInstance {
      return TypedNull(value.type)
   }

   override val instructionType = Instruction.InstructionType.FILTER

}

class ProcessInstructionExecutor : InstructionExecutor {
   override val instructionType = Instruction.InstructionType.PROCESS

   override fun execute(instruction: Instruction, value: TypedInstance): TypedInstance {
      val processor = TypeInstancePolicyProcessors.get(instruction.processor!!.name)
      return processor.process(value, instruction.processor!!.args)
   }
}

