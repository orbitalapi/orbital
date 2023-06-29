package io.vyne.cockpit.core.schemas.editor

import io.vyne.schema.publisher.loaders.*
import io.vyne.schemaServer.editor.*
import io.vyne.schemas.SavedQuery
import io.vyne.spring.config.ExcludeFromOrbitalStation
import io.vyne.spring.http.handleFeignErrors
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@ExcludeFromOrbitalStation
@RestController
class SchemaEditorServiceFacade(
   private val schemaEditorApi: SchemaEditorApi,
) {
   @PostMapping(path = ["/api/types/{typeName}/owner"])
   fun updateDataOwner(
      @PathVariable typeName: String,
      @RequestBody request: UpdateDataOwnerRequest
   ): Mono<AddChangesToChangesetResponse> {
      return handleFeignErrors { schemaEditorApi.updateDataOwnerOnType(typeName, request) }
   }

   @PostMapping(path = ["/api/types/{typeName}/annotations"])
   fun updateAnnotationsOnType(
      @PathVariable typeName: String,
      @RequestBody request: UpdateTypeAnnotationRequest
   ): Mono<AddChangesToChangesetResponse> {
      return handleFeignErrors { schemaEditorApi.updateAnnotationsOnType(typeName, request) }
   }

   @PostMapping("/api/repository/changeset/create")
   fun createChangeset(
      @RequestBody request: StartChangesetRequest
   ): Mono<CreateChangesetResponse> {
      return handleFeignErrors { schemaEditorApi.createChangeset(request) }
   }

   @PostMapping("/api/repository/changeset/add")
   fun addChangesToChangeset(
      @RequestBody request: AddChangesToChangesetRequest
   ): Mono<AddChangesToChangesetResponse> {
      return handleFeignErrors { schemaEditorApi.addChangesToChangeset(request) }
   }

   @PostMapping("/api/repository/changeset/finalize")
   fun finalizeChangeset(
      @RequestBody request: FinalizeChangesetRequest
   ): Mono<FinalizeChangesetResponse> {
      return handleFeignErrors { schemaEditorApi.finalizeChangeset(request) }
   }

   @PutMapping("/api/repository/changeset/update")
   fun updateChangeset(
      @RequestBody request: UpdateChangesetRequest
   ): Mono<UpdateChangesetResponse> {
      return handleFeignErrors { schemaEditorApi.updateChangeset(request) }
   }

   @PostMapping("/api/repository/changesets")
   fun getAvailableChangesets(
      @RequestBody request: GetAvailableChangesetsRequest
   ): Mono<AvailableChangesetsResponse> {
      return handleFeignErrors { schemaEditorApi.getAvailableChangesets(request) }
   }

   @PostMapping("/api/repository/changesets/active")
   fun setActiveChangeset(
      @RequestBody request: SetActiveChangesetRequest
   ): Mono<SetActiveChangesetResponse> {
      return handleFeignErrors { schemaEditorApi.setActiveChangeset(request) }
   }

   @PostMapping("/api/repository/queries")
   fun saveQuery(@RequestBody request: SaveQueryRequest): Mono<SavedQuery> =
      handleFeignErrors { schemaEditorApi.saveQuery(request) }
}
