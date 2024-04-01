package com.orbitalhq.schemaServer.changelog

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.orbitalhq.UnversionedPackageIdentifier
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.QualifiedNameAsStringDeserializer
import com.orbitalhq.schemas.QualifiedNameAsStringSerializer
import java.time.Instant

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
   MetadataChanged,

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
