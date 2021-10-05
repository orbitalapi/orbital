package io.vyne.schemaServer.editor

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.vyne.VersionedSource
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.QualifiedNameAsStringDeserializer
import io.vyne.schemas.QualifiedNameAsStringSerializer
import lang.taxi.CompilationMessage
import org.springframework.web.bind.annotation.PathVariable
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

   @PostMapping(path = ["/api/types/{typeName}/annotations"])
   fun updateAnnotationsOnType(
      @PathVariable typeName: String,
      @RequestBody request: UpdateTypeAnnotationRequest
   ): Mono<SchemaEditResponse>
}


data class SchemaEditRequest(
   val edits: List<VersionedSource>
)

data class SchemaEditResponse(
   val success: Boolean,
   val messages: List<CompilationMessage>
)


data class UpdateTypeAnnotationRequest(
   @JsonDeserialize(contentUsing = QualifiedNameAsStringDeserializer::class)
   @JsonSerialize(contentUsing = QualifiedNameAsStringSerializer::class)
   val annotations: List<QualifiedName>
)
