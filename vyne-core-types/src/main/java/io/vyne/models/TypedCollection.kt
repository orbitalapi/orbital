package io.vyne.models

import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import lang.taxi.ImmutableEquality
import lang.taxi.utils.takeHead
import mu.KotlinLogging


data class TypedCollection(
   override val type: Type,
   override val value: List<TypedInstance>,
   override val source: DataSource = MixedSources
) : List<TypedInstance> by value, TypedInstance {
   private val equality = ImmutableEquality(this, TypedCollection::type, TypedCollection::value)

   override fun toString(): String {
      return "TypedCollection(type=${type.qualifiedName.longDisplayName}, value=$value)"
   }
   operator fun get(key: String): TypedInstance {
      val (thisPart, remaining) = key.split(".")
         .takeHead()
      val requestedIndex = thisPart.removeSurrounding("[", "]").toInt()
      val thisItem = this[requestedIndex]
      val remainingPath = remaining.joinToString(".")
      return when {
         remaining.isEmpty() -> thisItem
         thisItem is TypedCollection -> thisItem.get(remainingPath)
         thisItem is TypedObject -> thisItem.get(remainingPath)
         else -> error("Don't know how to navigate path $remainingPath for type ${thisItem::class.simpleName}")
      }

   }

   override fun subList(fromIndex: Int, toIndex: Int): TypedCollection {
      return TypedCollection(this.type, value.subList(fromIndex, toIndex), source)
   }

   init {
      require(type.isCollection) {
         "Type ${type.name} was passed to TypedCollection, but it is not a collection type.  Call TypedCollection.arrayOf(...) instead"
      }
   }

   val memberType: Type
      get() {
         return type.typeParameters[0]
      }

   companion object {
      private val logger = KotlinLogging.logger {}
      fun arrayOf(
         collectionType: Type,
         value: List<TypedInstance>,
         source: DataSource = MixedSources.singleSourceOrMixedSources(value)
      ): TypedCollection {
         return TypedCollection(collectionType.asArrayType(), value, source)
      }

      fun empty(type: Type): TypedCollection {
         return TypedCollection(type, emptyList())
      }

      /**
       * Constructs a TypedCollection by interrogating the contents of the
       * provided list.
       * If the list is empty, then an exception is thrown
       */
      fun from(
         populatedList: List<TypedInstance>,
         source: DataSource = MixedSources.singleSourceOrMixedSources(populatedList)
      ): TypedCollection {
         // TODO : Find the most compatiable abstract type.
         val types = populatedList.map { it.type.resolveAliases() }.distinct()
         if (types.isEmpty()) {
            error("An empty list was passed, where a populated list was expected.  Cannot infer type.")
         }
         val commonType = types.first().commonTypeAncestor(types)
         return arrayOf(commonType, populatedList, source)
      }
      /**
       * If all the elements are TypedCollections, then the result is a single TypedCollection
       * with all the element flattened.
       *
       * Otherwise, the list is returned as-is
       */
      fun flatten(
         populatedList: List<TypedInstance>,
         source: DataSource = MixedSources.singleSourceOrMixedSources(populatedList)
      ):TypedCollection {
         if (populatedList.isEmpty()) {
            // Hmmm .. this is what the old code used to do, but I suspect this will
            // throw an error, as we can't know what the type is.
            return TypedCollection.from(populatedList, source)
         }
         return if (populatedList.all { it is TypedCollection }) {
            val nestedList = populatedList as List<TypedCollection>
            TypedCollection.from(nestedList.flatten(), source)
         } else {
            TypedCollection.from(populatedList, source)
         }
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

   override fun equals(other: Any?): Boolean {
      // Don't call equality.equals() here, as it's too slow.
      // We need a fast, non-reflection based implementation.
      if (this === other) return true
      if (other == null) return false
      if (this.javaClass !== other.javaClass) return false
      val otherCollection = other as TypedCollection
      // It's cheap to check the hashcodes
      if (this.hashCode() != other.hashCode()) return false
      return this.value == otherCollection.value
   }
   override fun hashCode(): Int = equality.hash()
}
