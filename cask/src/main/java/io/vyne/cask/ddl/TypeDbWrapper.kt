package io.vyne.cask.ddl

import de.bytefish.pgbulkinsert.row.SimpleRow
import de.bytefish.pgbulkinsert.row.SimpleRowWriter
import io.vyne.cask.ingest.CaskEntityMutatingMessage
import io.vyne.cask.ingest.CaskIdColumnValue
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

class TypeDbWrapper(val type: VersionedType, schema: Schema) {
   private val postgresDdlGenerator = PostgresDdlGenerator()
   val columns = postgresDdlGenerator.generateDdl(type, schema).columns
   val tableName = PostgresDdlGenerator.tableName(type)
   val rowWriterTable = SimpleRowWriter.Table(tableName, *columns.map { it.name }.toTypedArray())

   fun write(rowWriter: SimpleRow, attributeSet: InstanceAttributeSet): CaskEntityMutatingMessage {
      columns.map { column ->
         val value = column.readValue(attributeSet)
         value?.run { column.write(rowWriter, value) }
      }

      //Set the synthetic PK value
      val uuid = SyntheticPrimaryKeyColumn.readValue(attributeSet)!!
      SyntheticPrimaryKeyColumn.write(rowWriter, uuid)

      return CaskEntityMutatingMessage(
         tableName,
         listOf(CaskIdColumnValue(columnName = PostgresDdlGenerator.CASK_ROW_ID_COLUMN_NAME, value = uuid)),
         attributeSet
      )

   }

   fun upsert(template: JdbcTemplate, instance: InstanceAttributeSet): CaskEntityMutatingMessage {
      val (upsertInformation, idColumnValues) = postgresDdlGenerator.generateUpsertDml(type, instance)
      template.execute(upsertInformation)
      return CaskEntityMutatingMessage(
         tableName,
         idColumnValues,
         instance
      )
   }
}
