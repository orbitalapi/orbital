package io.vyne.queryService.policies

import com.fasterxml.jackson.annotation.JsonProperty
import io.vyne.schemas.Policy
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.fqn
import lang.taxi.policies.*

data class PolicyDto(
   val name: QualifiedName,
   val targetTypeName: QualifiedName,
   val ruleSets: List<RuleSetDto>
) {
   companion object {
      fun from(policy: Policy): PolicyDto {
         return PolicyDto(policy.name,
            policy.targetType.name,
            policy.ruleSets.map { RuleSetDto.from(it) }
         )
      }
   }
}

data class RuleSetDto(
   val scope: PolicyScope,
   val statements: List<PolicyStatementDto>
) {
   companion object {
      fun from(ruleSet: RuleSet): RuleSetDto {
         return RuleSetDto(ruleSet.scope, ruleSet.statements.map { PolicyStatementDto.from(it) })
      }
   }
}

data class PolicyStatementDto(
   val condition: ConditionDto,
   val instruction: Instruction
) {
   companion object {
      fun from(policyStatement: PolicyStatement): PolicyStatementDto {
         val condition: ConditionDto = when (policyStatement.condition) {
            is ElseCondition -> ElseConditionDto()
            is CaseCondition -> CaseConditionDto.from(policyStatement.condition as CaseCondition)
            else -> error("Unhandled condition type")
         }
         return PolicyStatementDto(condition, policyStatement.instruction)
      }
   }
}

interface ConditionDto {
   val type: String
}

class ElseConditionDto : ConditionDto {
   @get:JsonProperty
   override val type: String = "else"
}

data class CaseConditionDto(
   val lhSubject: SubjectDto,
   val operator: Operator,
   val rhSubject: SubjectDto
) : ConditionDto {
   @get:JsonProperty
   override val type: String = "case"

   companion object {
      fun from(caseCondition: CaseCondition): CaseConditionDto {
         return CaseConditionDto(
            lhSubject = SubjectDto.from(caseCondition.lhSubject),
            operator = caseCondition.operator,
            rhSubject = SubjectDto.from(caseCondition.rhSubject)
         )
      }
   }
}

interface SubjectDto {
   val type: String

   companion object {
      fun from(subject: Subject): SubjectDto {
         return when (subject) {
            is LiteralSubject -> LiteralSubjectDto(subject.value)
            is LiteralArraySubject -> LiteralArraySubjectDto(subject.values)
            is RelativeSubject -> RelativeSubjectDto(subject)
         }
      }
   }

}

data class RelativeSubjectDto(val source: RelativeSubject.RelativeSubjectSource, val targetTypeName: QualifiedName) : SubjectDto {
   @get:JsonProperty
   override val type: String = "RelativeSubject"

   constructor(subject: RelativeSubject) : this(subject.source, subject.targetType.qualifiedName.fqn())
}

data class LiteralArraySubjectDto(val values: List<Any>) : SubjectDto {
   @get:JsonProperty
   override val type: String = "LiteralArraySubject"
}

data class LiteralSubjectDto(val value: Any?) : SubjectDto {
   @get:JsonProperty
   override val type: String = "LiteralSubject"
}
