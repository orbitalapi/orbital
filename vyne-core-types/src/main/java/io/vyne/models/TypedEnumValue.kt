package io.vyne.models

import io.vyne.schemas.*

class TypedEnumValue(override val type: Type, private val enumValue: EnumValue, private val typeCache: TypeCache,
                     override val source: DataSource
) : TypedInstance {
   override val value: Any = enumValue.value

   val name: String = enumValue.name
   override fun withTypeAlias(typeAlias: Type): TypedInstance {
      return TypedEnumValue(typeAlias, enumValue, typeCache, source)
   }

   val synonyms: List<TypedEnumValue> by lazy {
      val t = this.enumValue.synonyms.map { synonymName ->
         val enumTypeName = synonymName.synonymFullQualifiedName()
         val enumValue = synonymName.synonymValue()
         val enumType = typeCache.type(enumTypeName)
         enumType.enumTypedInstance(enumValue)
      }
      t
   }

   override fun valueEquals(valueToCompare: TypedInstance): Boolean {
      if (valueToCompare !is TypedEnumValue) {
         return false
      }

      if (this.type.resolvesSameAs(valueToCompare.type)) {
         return this.name == valueToCompare.name
      }

      TODO()
   }

}
