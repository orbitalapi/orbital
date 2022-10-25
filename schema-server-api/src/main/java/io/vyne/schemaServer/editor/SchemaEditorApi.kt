package io.vyne.schemaServer.editor

import io.vyne.PackageIdentifier
import io.vyne.VersionedSource
import io.vyne.schemas.Metadata
import lang.taxi.CompilationMessage
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Mono

@ReactiveFeignClient("\${vyne.schema-server.name:schema-server}", qualifier = "schemaEditorFeignClient")
interface SchemaEditorApi {
   @PostMapping("/api/repository/editable/sources")
   fun submitEdits(
      @RequestBody request: SchemaEditRequest
   ): Mono<SchemaEditResponse>

   @PostMapping(path = ["/api/repository/types/{typeName}/annotations"])
   fun updateAnnotationsOnType(
      @PathVariable typeName: String,
      @RequestBody request: UpdateTypeAnnotationRequest
   ): Mono<SchemaEditResponse>

   // As per below - shouldn't be part of the Schema editing API
   @PostMapping(path = ["/api/repository/types/{typeName}/owner"])
   fun updateDataOwnerOnType(
      @PathVariable typeName: String,
      @RequestBody request: UpdateDataOwnerRequest
   ): Mono<SchemaEditResponse>

   @GetMapping("/api/repository/editable")
   fun getEditorConfig(): Mono<EditableRepositoryConfig>
}


/**
 * Provides the details of repositories that are
 * editablele (described by the package Ids of those
 * repositories).
 *
 */
data class EditableRepositoryConfig(
   val editablePackages: List<PackageIdentifier>
) {
   val editingEnabled: Boolean = editablePackages.isNotEmpty()

   // Short term workaround...
   // FOr now, only support editing of a single package.
   // However, we'll need to allow mutliple editable pacakages,
   // and edit requests will have to tell us which package the
   // edit should go to.

   fun getDefaultEditorPackage():PackageIdentifier {
      return when (editablePackages.size) {
         0 -> error("There are no packages configured to be editable")
         1 -> editablePackages.single()
         else -> error("There are multiple packages defined as editable. Pick one, mofo.")
      }
   }
}

data class SchemaEditRequest(
   val packageIdentifier: PackageIdentifier,
   val edits: List<VersionedSource>
)

data class SchemaEditResponse(
   val success: Boolean,
   val messages: List<CompilationMessage>
)


data class UpdateTypeAnnotationRequest(
   val annotations: List<Metadata>
)


/**
 * This shouldn't be part of the schema server, since these are just annotations.
 * But the API for mutating annotations is too compelx to build right now
 */
data class UpdateDataOwnerRequest(val id: String, val name: String)
