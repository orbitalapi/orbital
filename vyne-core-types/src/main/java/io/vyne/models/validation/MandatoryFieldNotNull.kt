package io.vyne.models.validation

import io.vyne.models.DeferredTypedInstance
import io.vyne.models.TypedCollection
import io.vyne.models.TypedEnumValue
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.models.TypedObject
import io.vyne.models.TypedValue
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Field
import lang.taxi.accessors.PathBasedAccessor

private fun getMandatoryFieldsWithNulls(typedObject: TypedObject): List<Pair<AttributeName, String?>> {
   return typedObject.type.attributes.entries
      .filter { it.value.nullable }
      .filter { typedObject.getValue(it.key) is TypedNull }
      .map { it.key to it.value.accessorPath()}
}

private fun Field.accessorPath(): String? {
   return when(this.accessor) {
      null -> null
      is PathBasedAccessor -> this.accessor.path
      else -> null
   }
}

private fun Pair<AttributeName, String?>.toLogString(): String = if (second == null) this.first else "$first (path = $second)}"

private fun process(typedInstance: TypedInstance): String? {
   return when (typedInstance) {
      is TypedObject -> {
         val mandatoryFieldsWithNullValue = getMandatoryFieldsWithNulls(typedInstance)
         if (mandatoryFieldsWithNullValue.isEmpty()) {
            return null
         }
         "The fields \"${mandatoryFieldsWithNullValue.joinToString("\", \"") { "${it.toLogString()}" }}\" are mandatory but there is no value provided."
      }

      is TypedCollection -> {
         val errors = typedInstance
            .mapIndexed { index, typedObject -> index to process(typedObject) }
            .filter { it.second != null }
         if (errors.isEmpty()) {
            return null
         }
         val errorsByItem = errors.joinToString("\n") { "Item ${it.first}: ${it.second}" }
         """The following items in the collection had empty values:
                  |$errorsByItem
               """.trimMargin()
      }

      is TypedNull -> return null
      is TypedValue -> return null
      is DeferredTypedInstance -> return null
      is TypedEnumValue -> return null
      else -> {
         error("Unable to validate an unknown TypedInstance subtype: ${typedInstance::class.simpleName}.")
      }
   }
}

val MandatoryFieldNotNull = ValidationRuleProcessor { typedInstance, violationHandlers ->
   val error = process(typedInstance) ?: return@ValidationRuleProcessor true
   // We want to run all violation handlers first before checking if all of them return true or not
   violationHandlers.map { handler ->
      handler.handle(typedInstance, error)
   }.all { it }
}
