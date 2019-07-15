package io.vyne.models


class RawObjectMapper : TypedInstanceMapper {
   override fun map(typedInstance: TypedInstance): Any? = typedInstance.value
}

class TypeNamedInstanceMapper : TypedInstanceMapper {
   override fun map(typedInstance: TypedInstance): Any? = TypeNamedInstance(typedInstance.type.name, typedInstance.value)
   override fun handleUnwrapped(original: TypedInstance, value: Any?): Any? {
      return when (value) {
         is TypeNamedInstance -> value
         else -> TypeNamedInstance(original.type.name, value)
      }
   }
}

interface TypedInstanceMapper {
   fun map(typedInstance: TypedInstance): Any?
   fun handleUnwrapped(original: TypedInstance, value: Any?): Any? {
      return value
   }
}

class TypedInstanceConverter(private val mapper: TypedInstanceMapper) {

   private fun unwrapMap(valueMap: Map<String, Any>): Map<String, Any?> {
      return valueMap.map { (entryKey, entryValue) ->
         when (entryValue) {
            is TypedInstance -> entryKey to convert(entryValue)
            else -> entryKey to entryValue
         }
      }.toMap()
   }

   private fun unwrapCollection(valueCollection: Collection<*>):List<Any?> {
      return valueCollection.map { collectionMember ->
         when (collectionMember) {
            is TypedInstance -> convert(collectionMember)
            else -> collectionMember
         }
      }
   }

   fun convert(typedInstance: TypedInstance): Any? {
      val value = typedInstance.value
      return when (typedInstance) {
         is Map<*, *> -> mapper.handleUnwrapped(typedInstance, unwrapMap(value as Map<String, Any>))
         is Collection<*> -> unwrapCollection(value as Collection<*>)
         // TODO : There's likely other types that need unwrapping
         else -> mapper.map(typedInstance)
      }

   }
}
