package io.vyne.cask.ingest

import io.vyne.models.DataSource
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType

/**
 * A set of fields from a specific typed instance.
 * We use this because while the baseline persisted version
 * will represent the full entity, future projections are
 * partial sets of attributes that represent the diff of the schema
 */
data class InstanceAttributeSet(
   val type: VersionedType,
   val attributes: Map<String, TypedInstance>,
   val messageId: String
) {
   fun toTypedInstance(schema: Schema): TypedInstance {
      return TypedObject(
         schema.type(type.taxiType),
         attributes,
         CaskMessageDataSource(messageId)
      )
   }
}

data class CaskMessageDataSource(val messageId: String) : DataSource {
   override val name: String = "CaskMessageDataSource"
   override val id: String = messageId

}
