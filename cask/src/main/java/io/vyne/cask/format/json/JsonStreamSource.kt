package io.vyne.cask.format.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import io.vyne.cask.ingest.InstanceAttributeSet
import io.vyne.cask.ingest.StreamSource
import io.vyne.cask.timed
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType
import reactor.core.publisher.Flux
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

class JsonStreamSource(private val input: Flux<InputStream>,
                       private val versionedType: VersionedType,
                       private val schema: Schema,
                       private val readCacheDirectory: Path,
                       private val objectMapper: ObjectMapper) : StreamSource {

   // TODO LENS-47 save input stream to disk for replay
   val cachePath: Path? by lazy {
      null
//      Files.createFile(readCacheDirectory.resolve(versionedType.fullyQualifiedName))
   }
   private val mapper = JsonStreamMapper(versionedType, schema)

   override val stream: Flux<InstanceAttributeSet>
      get() {
         return input
            .map { stream -> timed("JsonStreamSource.read") { objectMapper.readTree(stream) } }
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
