package io.vyne.schemaServer.editor

import io.vyne.PackageSourceName
import io.vyne.VersionedSource
import io.vyne.schemas.QualifiedName

data class SavedQuery(
   val name: QualifiedName,
   val sources: List<VersionedSource>,
   val url: String? = null
)

data class SaveQueryRequest(
   val source: VersionedSource,
   val previousPath: PackageSourceName? = null,
   val changesetName: String = "",
)
