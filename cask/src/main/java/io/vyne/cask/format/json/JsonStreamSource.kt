package io.vyne.cask.format.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import io.vyne.cask.ingest.InstanceAttributeSet
import io.vyne.cask.ingest.StreamSource
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType
import reactor.core.publisher.Flux
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

class JsonStreamSource(val input: Flux<InputStream>,
                       val vyneType: VersionedType,
                       val schema: Schema,
                       val readCacheDirectory: Path,
                       val objectMapper: ObjectMapper) : StreamSource {

   // TODO LENS-47 save input stream to disk for replay
   val cachePath: Path by lazy {
      Files.createFile(readCacheDirectory.resolve(vyneType.fullyQualifiedName))
   }

   override val stream: Flux<InstanceAttributeSet>
      get() {
         val mapper = JsonStreamMapper(vyneType.type, schema)
         return input
            .map { stream -> objectMapper.readTree(stream) }
            .filter { record -> !record.isEmpty }
            .map { record ->
               // when
               when (record) {
                  is ArrayNode -> record.map { mapper.map(it) }
                  else ->
                     listOf(mapper.map(record))
               }
            }.flatMapIterable { it }
      }
}
