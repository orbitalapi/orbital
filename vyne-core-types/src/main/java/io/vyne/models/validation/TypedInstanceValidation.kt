package io.vyne.models.validation

import io.vyne.models.TypedInstance

typealias ValidationMessage = String

fun interface ViolationHandler {
   fun handle(typedInstance: TypedInstance, validationMessage: ValidationMessage): Boolean
}

fun interface ValidationRuleProcessor {
   fun execute(typedInstance: TypedInstance, violationHandlers: List<ViolationHandler>): Boolean
}

data class ValidationRule(
   val validationRuleProcessor: ValidationRuleProcessor,
   val violationHandlers: List<ViolationHandler>
) {
   constructor(validationRuleProcessor: ValidationRuleProcessor, violationHandlers: ViolationHandler) : this(
      validationRuleProcessor,
      listOf(violationHandlers)
   )
}

fun TypedInstance.validate(rules: List<ValidationRule>): Boolean {
   val failingRules = rules.filter { !it.validationRuleProcessor.execute(this, it.violationHandlers) }
   return failingRules.isEmpty()
}

fun TypedInstance.validate(rule: ValidationRule): Boolean {
   return this.validate(listOf(rule))
}


fun failValidationViolationHandler(): ViolationHandler {
   return ViolationHandler { _, _ -> false }
}

fun noOpViolationHandler(fn: (validationMessage: ValidationMessage) -> Unit): ViolationHandler {
   return ViolationHandler { _, validationError ->
      fn(validationError)
      false
   }
}

