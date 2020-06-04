package io.vyne.cask.format.csv

import io.vyne.cask.ingest.InstanceAttributeSet
import io.vyne.cask.timed
import io.vyne.models.TypedInstance
import io.vyne.models.VersionedTypedInstance
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import org.apache.commons.csv.CSVRecord
import java.util.concurrent.TimeUnit

class CsvStreamMapper(private val targetType: Type, private val schema: Schema) {
    private val versionedType = schema.versionedType(targetType.name)

    fun map(csvRecord: CSVRecord, nullValues: Set<String> = emptySet()): InstanceAttributeSet {
        val instance = timed("CsvStreamMapper.map", timeUnit = TimeUnit.MILLISECONDS) {
           TypedInstance.from(targetType, csvRecord, schema, nullValues = nullValues)
        }

        return InstanceAttributeSet(
           versionedType,
           instance.value as Map<String, TypedInstance>
        )
    }

}

// TODO : Emit this, and find some way of letting differnt
// parsing strategies provide their own metadata for reprocessing later.
data class CsvStreamedRecord(
        val index: Int,
        val instance: VersionedTypedInstance
)
