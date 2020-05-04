package io.vyne.cask.ddl

import de.bytefish.pgbulkinsert.row.SimpleRow
import de.bytefish.pgbulkinsert.row.SimpleRowWriter
import io.vyne.schemas.VersionedType
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.cask.ingest.InstanceAttributeSet
import lang.taxi.types.Field
import java.nio.file.Path

data class TypeMigration(
        val targetType: VersionedType,
        val fields: List<Field>,
        val predecessorType: VersionedType
)

class TypeDbWrapper(val type: VersionedType, schema: TaxiSchema, cachePath: Path?, typeMigration: TypeMigration?) {
    fun write(rowWriter: SimpleRow, attributeSet: InstanceAttributeSet) {
        columns.map { column ->
            val value = attributeSet.attributes.getValue(column.field.name).value ?: TODO("Null handling")
            column.write(rowWriter, value)
        }

    }

    private val postgresDdlGenerator = PostgresDdlGenerator()
    val dropTableStatement: String = postgresDdlGenerator.generateDrop(type)
    val createTableStatement: TableGenerationStatement = postgresDdlGenerator.generateDdl(type, schema, cachePath, typeMigration)
    val columns = createTableStatement.columns
    val tableName = postgresDdlGenerator.tableName(type)

    val rowWriterTable = SimpleRowWriter.Table(tableName, *createTableStatement.columns.map { it.name }.toTypedArray())
}
