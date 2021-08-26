package io.vyne.models

import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import lang.taxi.Equality


data class TypedCollection(override val type: Type, override val value: List<TypedInstance>,
                           override val source: DataSource = MixedSources
) : List<TypedInstance> by value, TypedInstance {
   private val equality = Equality(this, TypedCollection::type, TypedCollection::value)
   override fun toString(): String {
      return "TypedCollection(type=${type.qualifiedName.longDisplayName}, value=$value)"
   }
   init {
      require(type.isCollection) {
         "Type ${type.name} was passed to TypedCollection, but it is not a collection type.  Call TypedCollection.arrayOf(...) instead"
      }
   }

   companion object {
      fun arrayOf(collectionType: Type, value: List<TypedInstance>): TypedCollection {
         return TypedCollection(collectionType.asArrayType(), value)
      }

      /**
       * Constructs a TypedCollection by interrogating the contents of the
       * provided list.
       * If the list is empty, then an exception is thrown
       */
      fun from(populatedList: List<TypedInstance>): TypedCollection {
         // TODO : Find the most compatiable abstract type.
         val types = populatedList.map { it.type.resolveAliases() }.distinct()
         if (types.isEmpty()) {
            error("An empty list was passed, where a populated list was expected.  Cannot infer type.")
         }
         val commonType = types.first().commonTypeAncestor(types)
         return TypedCollection.arrayOf(commonType, populatedList)
      }
   }

   override fun withTypeAlias(typeAlias: Type): TypedCollection {
      return TypedCollection(typeAlias, value)
   }

   override fun valueEquals(valueToCompare: TypedInstance): Boolean {
      if (valueToCompare !is TypedCollection) {
         return false
      }
      if (!this.type.resolvesSameAs(valueToCompare.type)) {
         return false
      }
      if (this.size != valueToCompare.size) {
         return false
      }
      this.forEachIndexed { index, typedInstance ->
         if (!typedInstance.valueEquals(valueToCompare[index])) {
            // Fail as soon as any values don't equal
            return false
         }
      }
      return true
   }

   fun parameterizedType(schema: Schema): Type {
      return schema.type("lang.taxi.Array<${type.name.parameterizedName}>")
   }

   override fun equals(other: Any?): Boolean = equality.isEqualTo(other)
   override fun hashCode(): Int = equality.hash()
}
