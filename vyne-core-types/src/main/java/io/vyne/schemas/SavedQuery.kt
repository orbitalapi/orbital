package io.vyne.schemas

import io.vyne.VersionedSource

data class SavedQuery(
   val name: QualifiedName,
   val sources: List<VersionedSource>,
   val url: String? = null
)

