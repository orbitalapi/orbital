package io.vyne.cask.format.csv

import io.vyne.models.TypedInstance
import io.vyne.models.VersionedTypedInstance
import io.vyne.schemas.Type
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.cask.ingest.InstanceAttributeSet
import io.vyne.cask.xtimed
import org.apache.commons.csv.CSVRecord
import java.util.concurrent.TimeUnit

class CsvStreamMapper(private val targetType: Type, private val schema: TaxiSchema) {
    private val versionedType = schema.versionedType(targetType.name)

    fun map(csvRecord: CSVRecord): InstanceAttributeSet {
        val instance = xtimed("createTypedInstance", timeUnit = TimeUnit.MICROSECONDS) {
           TypedInstance.from(targetType, csvRecord, schema)
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
