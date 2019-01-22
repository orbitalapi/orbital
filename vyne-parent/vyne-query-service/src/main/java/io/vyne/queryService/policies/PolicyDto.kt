package io.vyne.queryService.policies

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
         return PolicyStatementDto(condition,policyStatement.instruction)
      }
   }
}

interface ConditionDto

class ElseConditionDto : ConditionDto
data class CaseConditionDto(
   val lhSubject: SubjectDto,
   val operator: Operator,
   val rhSubject: SubjectDto
) : ConditionDto {
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
   companion object {
      fun from(subject: Subject): SubjectDto {
         return when(subject) {
            is LiteralSubject -> LiteralSubjectDto(subject.value)
            is LiteralArraySubject -> LiteralArraySubjectDto(subject.values)
            is RelativeSubject -> RelativeSubjectDto(subject)
         }
      }
   }

}

data class RelativeSubjectDto(val source: RelativeSubject.RelativeSubjectSource, val targetTypeName: QualifiedName):SubjectDto {
   constructor(subject: RelativeSubject) : this(subject.source, subject.targetType.qualifiedName.fqn())
}

data class LiteralArraySubjectDto(val values: List<Any>) : SubjectDto
data class LiteralSubjectDto(val value: Any?) : SubjectDto
