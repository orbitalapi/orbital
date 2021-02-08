package io.vyne.cask.format.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import io.vyne.cask.ingest.InstanceAttributeSet
import io.vyne.cask.ingest.StreamSource
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType
import reactor.core.publisher.Flux
import java.io.InputStream

class JsonStreamSource(
   private val inputStream: InputStream,
   private val versionedType: VersionedType,
   private val schema: Schema,
   override val messageId: String,
   private val objectMapper: ObjectMapper
) : StreamSource {
   constructor(
      input: Flux<out InputStream>,
      versionedType: VersionedType,
      schema: Schema,
      messageId: String,
      objectMapper: ObjectMapper
   ) : this(input.blockFirst()!!, versionedType, schema, messageId, objectMapper)

   private val mapper = JsonStreamMapper(versionedType, schema)

   override val records: List<InstanceAttributeSet>
      get() {
         val jsonNode = objectMapper.readTree(inputStream)
         return when {
            jsonNode.isEmpty -> emptyList()
            jsonNode is ArrayNode -> jsonNode.map { mapper.map(it, messageId) }
            else -> listOf(mapper.map(jsonNode, messageId))
         }
      }


   override val stream: Flux<InstanceAttributeSet>
      get() {
         TODO()
//         return input
//            .map { stream ->
//               val jsonNode = objectMapper.readTree(stream)
//               when {
//                  jsonNode.isEmpty -> emptyList()
//                  jsonNode is ArrayNode -> jsonNode.map { mapper.map(it, messageId) }
//                  else -> listOf(mapper.map(jsonNode, messageId))
//               }
//            }.flatMapIterable { it }
      }
}
