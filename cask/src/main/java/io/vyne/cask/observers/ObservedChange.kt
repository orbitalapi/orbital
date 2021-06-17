package io.vyne.cask.observers

import io.vyne.cask.ingest.CaskEntityMutatingMessage
import io.vyne.models.RawObjectMapper
import io.vyne.models.TypedInstance
import io.vyne.models.TypedInstanceConverter
import io.vyne.schemas.Schema

/**
 * Encapsulates the data that is published to Kafka for models annotated with @ObserveChanges
 * @param ids Map of field names vs values for fields annotated with @PrimaryKey annotation. If the model doesn't have any PKs
 *  it contains a singleton map with a key cask_raw_id and cask generated UUID value.
 * @param current Map of fieldname vs TypedInstance.value for the ingested model value.
 * @param old Map of old values for the currently ingested TypedObject. For models without PK fields, this will be null.
 * Otherwise, it contains fieldname vs TypedInstance.value for the previous TypedObject with given PKs. If there is no such
 * previous TypedObject for the given PKs, the map still populated, but all 'values' will be null.
 */
data class ObservedChange(val ids: Map<String, Any>, val current: Map<String, Any?>, val old: Map<String, Any?>? = null) {
   companion object {
      fun fromCaskEntityMutatingMessage(schema: Schema, caskEntityMutatingMessage: CaskEntityMutatingMessage): ObservedChange {
         val instanceAttributeSet = caskEntityMutatingMessage.attributeSet
         val vyneType = schema.type(instanceAttributeSet.type.taxiType)
         val oldValues = caskEntityMutatingMessage.oldValues?.map { oldValue ->
            val maybeField = vyneType.attributes[oldValue.key]
            val maybeTypedInstanceValue = maybeField?.let { field ->
              val convertedValue = converter.convert(TypedInstance.from(schema.type(field.type), oldValue.value, schema))
               oldValue.key to convertedValue
            }
            maybeTypedInstanceValue ?: (oldValue.key to oldValue.value)
         }?.toMap()

         val current = instanceAttributeSet.attributes.map { attribute -> attribute.key to converter.convert(attribute.value) }.toMap()
         return ObservedChange(
            ids = caskEntityMutatingMessage.identity.map { it.columnName to it.value}.toMap(),
            current = current,
            old = oldValues)
      }
      private val converter = TypedInstanceConverter(RawObjectMapper)
       fun serialize(item: TypedInstance): Any? {
         return converter.convert(item)
      }
   }
}
