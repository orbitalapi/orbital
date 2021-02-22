package io.vyne.cask.format.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import io.vyne.cask.ingest.InstanceAttributeSet
import io.vyne.cask.ingest.StreamSource
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType
import java.io.InputStream

class JsonStreamSource(
   private val inputStream: InputStream,
   private val versionedType: VersionedType,
   private val schema: Schema,
   override val messageId: String,
   private val objectMapper: ObjectMapper
) : StreamSource {
   private val mapper = JsonStreamMapper(versionedType, schema)
   private var sequenceCreated: Boolean = false

   // Can possibly optimise this further so that json is streamed out of the mapper.
   // But for now, the records list seems performant enough
   override fun sequence(): Sequence<InstanceAttributeSet> {
      require(!sequenceCreated) { "This sequence has already been consumed" }
      sequenceCreated = true
      val jsonNode = objectMapper.readTree(inputStream)
      return when {
         jsonNode.isEmpty -> emptySequence()
         jsonNode is ArrayNode -> jsonNode.map { mapper.map(it, messageId) }.asSequence()
         else -> listOf(mapper.map(jsonNode, messageId)).asSequence()
      }
   }

}
