package io.vyne.models

import io.vyne.schemas.EnumValue
import io.vyne.schemas.Type
import io.vyne.schemas.TypeCache
import lang.taxi.types.EnumType
import lang.taxi.types.EnumValueQualifiedName

/**
 * Indicates if a value from a TypedEnumValue should use the name of the enum, or the value.
 * (ie., given an enum of NZ("New Zealand"), should we use NZ or New Zealand
 */
enum class EnumValueKind {
   NAME,
   VALUE;

   companion object {
      fun from(value: TypedValue, taxiType: EnumType): EnumValueKind {

         return when {
            taxiType.hasValue(value.value) -> VALUE
            else -> NAME

         }
      }
   }

}

class TypedEnumValue(
   override val type: Type, val enumValue: EnumValue, private val typeCache: TypeCache,
   override val source: DataSource
) : TypedInstance {
   override val value: Any = enumValue.value
   private val enumType: EnumType = type.taxiType as EnumType
   val enumValueQualifiedName: EnumValueQualifiedName = enumType.ofName(enumValue.name).qualifiedName


   val name: String = enumValue.name
   override fun withTypeAlias(typeAlias: Type): TypedInstance {
      return TypedEnumValue(typeAlias, enumValue, typeCache, source)
   }

   /**
    * Provides the synonyms for this enum, mapped to a typed value, using either the
    * name or the value.
    * This allows for like-for-like mapping of synonyms, when a source is provided using
    * either the name or the value
    */
   fun synonymsAsTypedValues(valueKind: EnumValueKind): List<TypedValue> {
      return typeCache.enumSynonymsAsTypedValues(this, valueKind)
   }

   val synonyms: List<TypedEnumValue> by lazy {
      typeCache.enumSynonyms(this)
   }

   override fun valueEquals(valueToCompare: TypedInstance): Boolean {
      if (valueToCompare !is TypedEnumValue) {
         return false
      }

      if (this.type.resolvesSameAs(valueToCompare.type)) {
         return this.name == valueToCompare.name
      }

      TODO("valueEquals on TypedEnumValue not implemented")
   }

   fun asTypedValue(enumValueKind: EnumValueKind): TypedValue {
      val value = when (enumValueKind) {
         EnumValueKind.NAME -> enumValue.name
         EnumValueKind.VALUE -> enumValue.value
      }
      return TypedValue.from(
         this.type,
         value,
         false,
         MappedSynonym(this)
      )
   }

}

object EnumSynonyms {
   fun fromTypeValue(instance: TypedValue): List<TypedValue> {
      require(instance.type.isEnum) { "${instance.type.name} is not an enum" }
      val underlyingEnumType = instance.type.taxiType as EnumType
      // Instantiate with either name or value depending on what we have as input
      val enumValueKind = EnumValueKind.from(instance, underlyingEnumType)
      return instance.type.enumTypedInstanceOrNull(instance.value)
         ?.synonymsAsTypedValues(enumValueKind)
         ?: emptyList()
   }

   fun enumSynonymsFromTypedValue(instance: TypedValue): List<TypedEnumValue> {
      require(instance.type.isEnum) { "${instance.type.name} is not an enum" }
      return instance.type.enumTypedInstanceOrNull(instance.value)
         ?.synonyms
         ?: emptyList()
   }
}
