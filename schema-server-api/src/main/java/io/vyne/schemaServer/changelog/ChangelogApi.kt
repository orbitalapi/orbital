package io.vyne.schemaServer.changelog

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.vyne.UnversionedPackageIdentifier
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.QualifiedNameAsStringDeserializer
import io.vyne.schemas.QualifiedNameAsStringSerializer
import org.springframework.web.bind.annotation.GetMapping
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Mono
import java.time.Instant

// Please note that feign client name is specified through vyne.taxonomyServer.name
// We can use vyne.schema-server.name here as there is another @ReactiveFeignClient - SchemaEditorApi
// which uses vyne.schema-server.name configuration option. Due to a bug in playtika, qualifier is not considered properly
// to distinguish these definitions and hence we need to use a different configuration setting here.
@ReactiveFeignClient("\${vyne.changelog-server.name:schema-server}", qualifier = "schemaChangeLogFeignClient", )
interface ChangelogApi {
   @GetMapping("/api/schema/changelog")
   fun getChangelog(): Mono<List<ChangeLogEntry>>
}


data class ChangeLogEntry(
   val timestamp: Instant,
   val affectedPackages: List<UnversionedPackageIdentifier>,
   val diffs: List<ChangeLogDiffEntry>
)

enum class DiffKind {
   TypeAdded,
   TypeRemoved,
   ModelAdded,
   ModelRemoved,
   ModelChanged,
   DocumentationChanged,
   FieldAddedToModel,
   FieldRemovedFromModel,

   ServiceAdded,
   ServiceRemoved,
   ServiceChanged,

   OperationAdded,
   OperationRemoved,

   OperationMetadataChanged,
   OperationParametersChanged,
   OperationReturnValueChanged
}

data class ChangeLogDiffEntry(
   val displayName: String,
   val kind: DiffKind,
   @JsonSerialize(using = QualifiedNameAsStringSerializer::class)
   @JsonDeserialize(using = QualifiedNameAsStringDeserializer::class)
   val schemaMember: QualifiedName,
   val children: List<ChangeLogDiffEntry> = emptyList(),

   // Should be a String or Qualified Name
   val oldDetails: Any? = null,
   // Should be a String or Qualified Name
   val newDetails: Any? = null,
)
