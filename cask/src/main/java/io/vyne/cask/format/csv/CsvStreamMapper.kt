package io.vyne.cask.format.csv

import io.vyne.cask.ingest.InstanceAttributeSet
import io.vyne.cask.timed
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.VersionedTypedInstance
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType
import org.apache.commons.csv.CSVRecord
import java.util.concurrent.TimeUnit

class CsvStreamMapper(private val versionedType: VersionedType, private val schema: Schema) {
    fun map(csvRecord: CSVRecord, nullValues: Set<String> = emptySet(), messageId:String, logMappingTime: Boolean = false): InstanceAttributeSet {
        val instance = timed("CsvStreamMapper.map", log = logMappingTime, timeUnit = TimeUnit.MILLISECONDS) {// generates noise in tests
           TypedInstance.from(versionedType.type, csvRecord, schema, nullValues = nullValues, source = Provided)
        }

        return InstanceAttributeSet(
           versionedType,
           instance.value as Map<String, TypedInstance>,
           messageId
        )
    }

}
