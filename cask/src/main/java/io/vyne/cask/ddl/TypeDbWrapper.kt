package io.vyne.cask.ddl

import de.bytefish.pgbulkinsert.row.SimpleRow
import de.bytefish.pgbulkinsert.row.SimpleRowWriter
import io.vyne.cask.ingest.InstanceAttributeSet
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType
import lang.taxi.types.Field
import org.springframework.jdbc.core.JdbcTemplate
import java.nio.file.Path

data class TypeMigration(
        val targetType: VersionedType,
        val fields: List<Field>,
        val predecessorType: VersionedType
)

class TypeDbWrapper(val type: VersionedType, schema: Schema, cachePath: Path?, typeMigration: TypeMigration?) {
    fun write(rowWriter: SimpleRow, attributeSet: InstanceAttributeSet) {
        columns.map { column ->
            val value = attributeSet.attributes.getValue(column.field.name).value
            value?.run { column.write(rowWriter, value) }
        }

    }

   fun upsert(template: JdbcTemplate, instance: InstanceAttributeSet) {
      val upsertRowStatement: String = postgresDdlGenerator.generateUpsertDml(type, instance)
      template.execute(upsertRowStatement)
   }

    private val postgresDdlGenerator = PostgresDdlGenerator()
    val dropTableStatement: String = postgresDdlGenerator.generateDrop(type)
    val createTableStatement: TableGenerationStatement = postgresDdlGenerator.generateDdl(type, schema, cachePath, typeMigration)
    val columns = createTableStatement.columns
    val tableName = PostgresDdlGenerator.tableName(type)

    val rowWriterTable = SimpleRowWriter.Table(tableName, *createTableStatement.columns.map { it.name }.toTypedArray())
}
