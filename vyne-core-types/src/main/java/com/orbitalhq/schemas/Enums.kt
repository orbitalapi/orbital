package com.orbitalhq.schemas

import com.orbitalhq.models.DefinedInSchema
import com.orbitalhq.models.TypedEnumValue
import com.orbitalhq.models.TypedInstance

fun String.synonymFullyQualifiedName() = split(".").dropLast(1).joinToString(".")
fun String.synonymValue() = split(".").last()

fun String.toSynonymTypeAndValue():Pair<String,String> {
   return this.synonymFullyQualifiedName() to this.synonymValue()
}

data class EnumValue(val name: String, val value: Any,  val synonyms: List<String>, val typeDoc: String?, val typedValueSupplier: EnumValueSupplier) {
   override fun toString(): String {
      return "$name($value)"
   }
}

/**
 * Typed values in enums can't be supplied at the time of the type construction
 * because of some self-referential challenges.
 *
 * This interface is responsible for converting the EnumValue (coming from the
 * taxi schema) to an Orbital TypedEnumValue.
 */
sealed interface EnumValueSupplier {
   fun typedEnumValue(enumValue:EnumValue, type: Type, typeCache: TypeCache): TypedEnumValue
}

data object ScalarEnumValueSupplier : EnumValueSupplier {
   override fun typedEnumValue(enumValue: EnumValue, type: Type, typeCache: TypeCache): TypedEnumValue {
      return TypedEnumValue(type, enumValue, typeCache, DefinedInSchema)
   }
}

/**
 * Builds a TypedEnumValue when the value itself is an object.
 * Returns the value as a TypedInstance
 */
class ObjectEnumValueSupplier(private val schema: Schema) : EnumValueSupplier {
   override fun typedEnumValue(enumValue: EnumValue, type: Type, typeCache: TypeCache): TypedEnumValue {
      require(enumValue.value is lang.taxi.types.TypedValue) { "Expected a non-scalar value of a taxi TypedValue, butgot ${enumValue.value::class.simpleName}"}
      val value = TypedInstance.from(enumValue.value, schema, DefinedInSchema)
      return TypedEnumValue(type, enumValue.copy(value = value), typeCache, DefinedInSchema)
   }
}
