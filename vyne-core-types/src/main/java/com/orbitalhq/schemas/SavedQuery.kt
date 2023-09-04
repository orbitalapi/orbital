package com.orbitalhq.schemas

import com.orbitalhq.VersionedSource

data class SavedQuery(
   val name: QualifiedName,
   val sources: List<VersionedSource>,
   val url: String? = null
)

