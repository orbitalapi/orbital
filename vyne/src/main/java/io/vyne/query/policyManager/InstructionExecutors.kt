package io.vyne.query.policyManager

import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.models.TypedObject
import lang.taxi.policies.FilterInstruction
import lang.taxi.policies.Instruction
import lang.taxi.policies.PermitInstruction

interface InstructionExecutor<T : Instruction> {
   val instructionType: Instruction.InstructionType
   fun execute(instruction: T, value: TypedInstance): TypedInstance
}

object InstructionExecutors {
   private val executors = listOf(
      PermitInstructionExecutor(),
      FilterInstructionExecutor()
   ).associateBy { it.instructionType }

   fun <T : Instruction> get(instruction: T): InstructionExecutor<T> {
      return executors[instruction.type] as InstructionExecutor<T>?
         ?: error("No processor defined for instruction $instruction")
   }
}

class PermitInstructionExecutor : InstructionExecutor<PermitInstruction> {
   override val instructionType = Instruction.InstructionType.PERMIT
   override fun execute(instruction: PermitInstruction, value: TypedInstance): TypedInstance {
      return value
   }
}

class FilterInstructionExecutor : InstructionExecutor<FilterInstruction> {
   override fun execute(instruction: FilterInstruction, value: TypedInstance): TypedInstance {
      if (instruction.isFilterAll) {
         return TypedNull(value.type)
      } else {
         require(value is TypedObject) { "Cannot filter attributes of type ${value.type.fullyQualifiedName} as it is not an object" }
         val valueObject = value as TypedObject

         val replacements = instruction.fieldNames.map { fieldName ->
            val originalAttribute = valueObject[fieldName]
            fieldName to TypedNull(originalAttribute.type)
         }.toMap()
         return valueObject.copy(replacements)
      }
   }

   override val instructionType = Instruction.InstructionType.FILTER

}

// Processors are disabled:
// https://gitlab.com/vyne/vyne/issues/52
//class ProcessInstructionExecutor : InstructionExecutor {
//   override val instructionType = Instruction.InstructionType.PROCESS
//
//   override fun execute(instruction: Instruction, value: TypedInstance): TypedInstance {
//      val processor = TypeInstancePolicyProcessors.get(instruction.processor!!.name)
//      return processor.process(value, instruction.processor!!.args)
//   }
//}

