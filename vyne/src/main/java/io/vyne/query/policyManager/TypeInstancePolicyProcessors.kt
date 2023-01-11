package io.vyne.query.policyManager

import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.models.TypedValue
import io.vyne.schemas.QualifiedName

/**
 * Responsible for processing a value (ie., to mask it),
 * when declared as part of a data access policy.
 *
 * This is an early impl.
 *
 * It's likely that this will expand to allow lookup of user-provided
 * processors, either as functions, or declared within a schema in some
 * sort of scripting language.
 *
 * For now, it's a minimal implentation
 */
interface TypeInstancePolicyProcessor {

   val qualifiedName: QualifiedName
   fun process(input: TypedInstance, args: List<Any>): TypedInstance
}

object TypeInstancePolicyProcessors {
   private val processors = listOf<TypeInstancePolicyProcessor>(StringMaskingProcessor())
      .associateBy { it.qualifiedName }

   fun get(name: String): TypeInstancePolicyProcessor {
      return get(QualifiedName.from(name))
   }

   fun get(name: QualifiedName): TypeInstancePolicyProcessor {
      return processors[name] ?: error("No TypeInstancePolicyProcessor defined with name $name")
   }
}

class StringMaskingProcessor : TypeInstancePolicyProcessor {
   companion object {
       const val MASKED_VALUE = "******"
   }
   override val qualifiedName: QualifiedName = QualifiedName.from("vyne.StringMasker")

   override fun process(input: TypedInstance, args: List<Any>): TypedInstance {

      val propertiesToMaskArg = args[0]
      require(propertiesToMaskArg is List<*>) { "The first param must be an array of property names to mask" }
      val propertiesToMask = args[0] as List<String>
      require(input is TypedObject) { "Can only use a StringMaskingProcessor on a TypedObject" }
      val maskedProperties = input.value.map { (propertyName, value) ->
         if (propertiesToMask.contains(propertyName)) {
            val typedValue = value as TypedValue
            propertyName to typedValue.copy(value = MASKED_VALUE)
         } else {
            propertyName to value
         }
      }.toMap()
      return input.copy(suppliedValue = maskedProperties)
   }

}
