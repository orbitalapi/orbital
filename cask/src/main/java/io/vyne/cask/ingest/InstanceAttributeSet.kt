package io.vyne.cask.ingest

import io.vyne.models.TypedInstance
import io.vyne.schemas.VersionedType

/**
 * A set of fields from a specific typed instance.
 * We use this because while the baseline persisted version
 * will represent the full entity, future projections are
 * partial sets of attributes that represent the diff of the schema
 */
data class InstanceAttributeSet(
        val type: VersionedType,
        val attributes: Map<String, TypedInstance>
)
