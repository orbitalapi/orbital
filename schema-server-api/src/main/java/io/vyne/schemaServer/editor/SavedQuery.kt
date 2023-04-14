package io.vyne.schemaServer.editor

import io.vyne.PackageSourceName
import io.vyne.VersionedSource

data class SavedQuery(
   val source: VersionedSource
)

data class SaveQueryRequest(
   val source: VersionedSource,
   val previousPath: PackageSourceName? = null,
   val changesetName: String = "",
)
