package com.orbitalhq.history

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.Type

/**
 * Modifies a TypedCollection so that we serialize it's metadata, as well
 * as it's value.
 *
 * Normally, a collection is simply iterated.
 * But, that means we lose things like the data source, which tell us
 * where the collection came from.
 *
 * When serializing for Lineage, data source is important.
 */
private data class TypedCollectionAsValue(
   override val type: Type,
   override val value: Any,
   override val source: DataSource,
   override val nodeId: String
)  : TypedInstance {
   override val metadata: Map<String, Any> = emptyMap()
   override fun withTypeAlias(typeAlias: Type): TypedInstance {
      TODO("Not yet implemented")
   }

   override fun valueEquals(valueToCompare: TypedInstance): Boolean {
      TODO("Not yet implemented")
   }
}

/**
 * Jackson serializer with customisations specifically for lineage.
 * We treat TypedCollections differently when serializing for lineage,
 * as we want to capture their metadata as well as their value.
 */
object LineageJsonSerializer {
   val module = SimpleModule("LineageSerializerModule").addSerializer(
      TypedCollection::class.java, CollectionAsValueSerializer()
   )
   val objectMapper = jacksonObjectMapper()
      .registerModule(module)
      .findAndRegisterModules()
}


class CollectionAsValueSerializer : JsonSerializer<TypedCollection>() {
   override fun serialize(p0: TypedCollection, p1: JsonGenerator, p2: SerializerProvider) {
      val value = TypedCollectionAsValue(
         p0.type,
         p0.value,
         p0.source,
         p0.nodeId
      )
      p1.writeObject(value)
   }

}
