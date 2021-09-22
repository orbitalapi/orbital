package io.vyne.schemaServer.editor

import io.vyne.VersionedSource
import lang.taxi.CompilationMessage
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Mono

@ReactiveFeignClient("\${vyne.schema-server.name:schema-server}")
interface SchemaEditorApi {
   @PostMapping("/api/repository/editable/sources")
   fun submitEdits(
      @RequestBody request: SchemaEditRequest
   ): Mono<SchemaEditResponse>
}


data class SchemaEditRequest(
   val edits: List<VersionedSource>
)

data class SchemaEditResponse(
   val success: Boolean,
   val messages: List<CompilationMessage>
)
