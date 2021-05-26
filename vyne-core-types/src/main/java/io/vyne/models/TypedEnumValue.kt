package io.vyne.models

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vyne.schemas.EnumValue
import io.vyne.schemas.Type
import io.vyne.schemas.TypeCache
import io.vyne.utils.ImmutableEquality
import lang.taxi.packages.utils.log
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
      fun from(value: Any, taxiType: EnumType): EnumValueKind {
         return when {
            taxiType.hasExplicitValue(value) -> VALUE
            else -> NAME

         }
      }

      fun from(value: TypedValue, taxiType: EnumType): EnumValueKind {
         return from(value.value, taxiType)
      }
   }

}

// Note - these used to be cached and long-lived / shared across types.
// However, this breaks lineage, as the source needs to reflect where the value came from, which
// obviously differs for each instance.
// There was some performance related goal that was the rationale for the shared TypedEnumValue,
// but I forget what it is right now.  If perf blows after making these changes, come back and revisit.
data class TypedEnumValue(
   @JsonIgnore
   override val type: Type,
   @JsonIgnore
   val enumValue: EnumValue,
   private val typeCache: TypeCache,
   override val source: DataSource,
   private val valueKind: EnumValueKind = EnumValueKind.VALUE
) : TypedInstance {

   override fun toString(): String {
      return "${type.qualifiedName.longDisplayName}.$enumValue"
   }

   override val value: Any = if (valueKind == EnumValueKind.VALUE) enumValue.value else enumValue.name
   private val enumType: EnumType = type.taxiType as EnumType
   val enumValueQualifiedName: EnumValueQualifiedName = enumType.ofName(enumValue.name).qualifiedName

   private val equality = ImmutableEquality(this, TypedEnumValue::type, TypedEnumValue::enumValue)
   override fun hashCode(): Int = equality.hash()
   override fun equals(other: Any?): Boolean {
      return equality.isEqualTo(other)
   }

   val name: String = enumValue.name
   override fun withTypeAlias(typeAlias: Type): TypedInstance {
      return TypedEnumValue(typeAlias, enumValue, typeCache, source)
   }

   @get:JsonIgnore
   @delegate:JsonIgnore
   val synonyms: List<TypedEnumValue> by lazy {
      EnumSynonyms.build(this, typeCache, MappedSynonym(this), this.valueKind)
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
}


object EnumSynonyms {
   private data class SynonymBuildRequest(
      val fromValue: TypedEnumValue,
      val valueKind: EnumValueKind
   )

   private val synonymsBeingBuilt = mutableMapOf<SynonymBuildRequest, MutableList<TypedEnumValue>>()
   fun build(
      fromValue: TypedEnumValue,
      typeCache: TypeCache,
      dataSource: DataSource,
      valueKind: EnumValueKind
   ): List<TypedEnumValue> {

      // Originally used DataSource here too, but that creates issues with equality, as
      // the DataSource has a UUID
      val key = SynonymBuildRequest(fromValue, valueKind)

      synchronized(synonymsBeingBuilt) {
         if (synonymsBeingBuilt.containsKey(key)) {
            return synonymsBeingBuilt[key]!!
         } else {
            synonymsBeingBuilt.put(key, mutableListOf())
         }
      }
      log().info("Building synonyms for $key")

      val list = synonymsBeingBuilt[key]!!
      val synonyms = typeCache.enumSynonyms(fromValue)
      synonyms
         .mapTo(list) { it.copy(source = MappedSynonym(fromValue), valueKind = valueKind) }
//      synchronized(synonymsBeingBuilt) {
//         synonymsBeingBuilt.remove(key)
//      }
      return list
   }

   fun enumSynonymsFromTypedValue(instance: TypedValue): List<TypedEnumValue> {
      require(instance.type.isEnum) { "${instance.type.name} is not an enum" }
      val synonyms = instance.type.enumTypedInstanceOrNull(instance.value, instance.source)
         ?.synonyms
         ?: emptyList()
      return synonyms.map { it.copy(source = instance.source) }
   }
}

