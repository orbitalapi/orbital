package io.vyne.cask.ddl

import de.bytefish.pgbulkinsert.row.SimpleRow
import de.bytefish.pgbulkinsert.row.SimpleRowWriter
import io.vyne.cask.ingest.InstanceAttributeSet
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType
import lang.taxi.types.Field
import org.springframework.jdbc.core.JdbcTemplate

data class TypeMigration(
   val targetType: VersionedType,
   val fields: List<Field>,
   val predecessorType: VersionedType
)

data class TypeDbWrapper(val type: VersionedType, val schema: Schema) {
   fun write(rowWriter: SimpleRow, attributeSet: InstanceAttributeSet) {
      columns.map { column ->
         val value = column.readValue(attributeSet)
         value?.run { column.write(rowWriter, value) }
      }
   }

   fun upsert(template: JdbcTemplate, instance: InstanceAttributeSet) {
      val upsertRowStatement: String = postgresDdlGenerator.generateUpsertDml(type, instance)
      template.execute(upsertRowStatement)
   }

   private val postgresDdlGenerator = PostgresDdlGenerator()
   val upsertStatement: PostgresDdlGenerator.UpsertStatement by lazy {
      postgresDdlGenerator.generateUpsertWithPlaceholders(this.type)
   }

   val columns = PostgresDdlGenerator().generateDdl(type, schema).columns
   val tableName = PostgresDdlGenerator.tableName(type)
   val rowWriterTable = SimpleRowWriter.Table(tableName, *columns.map { it.name }.toTypedArray())
}
