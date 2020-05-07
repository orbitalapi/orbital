package io.vyne.cask.format.csv

import io.vyne.models.TypedInstance
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.cask.ddl.TypeMigration
import io.vyne.cask.ingest.InstanceAttributeSet
import io.vyne.cask.ingest.StreamSource
import io.vyne.cask.log
import lang.taxi.types.ColumnAccessor
import lang.taxi.types.Field
import reactor.core.publisher.Flux
import java.nio.file.Files
import java.nio.file.Path

class CsvBinaryCacheStreamSource(private val readCachePath: Path, private val migration: TypeMigration, private val schema: TaxiSchema) : StreamSource {
    init {
        require(Files.exists(readCachePath)) { "$readCachePath does not exist" }
        require(Files.isRegularFile(readCachePath)) { "$readCachePath is not a file" }
    }

    private val columnIndexesToRead: Map<Int, Field> = migration.fields
            .mapNotNull { field ->
                when (field.accessor) {
                    null -> log().warn("Field ${field.name} is not mapped to a column, and will be ignored").let { null }
                    is ColumnAccessor -> (field.accessor as ColumnAccessor).index to field
                    else -> log().warn("Field ${field.name} is mapped to an accessor of type ${field.accessor!!::class.simpleName} which cannot be handled by this PipelineSource, so will be ignored").let { null }
                }
            }.toMap()

    override val stream: Flux<InstanceAttributeSet>
        get() {
            return CsvBinaryReader().readAllValuesAtColumn(readCachePath, columnIndexesToRead.keys.sorted().toSortedSet())
                    .map { indicesToValues ->
                        val fieldValues = indicesToValues.map { (index, value) ->
                            // TODO : This won't work for supported reference based fields, like
                            // those with read conditions etc.
                            val columnField = columnIndexesToRead.getValue(index)
                            val vyneType = schema.type(columnField.type.qualifiedName)
                            columnField.name to TypedInstance.from(vyneType, value, schema)
                        }.toMap()
                       InstanceAttributeSet(migration.targetType, fieldValues)
                    }
        }
}
