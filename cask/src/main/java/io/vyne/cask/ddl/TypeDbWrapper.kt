package io.vyne.cask.ddl

import de.bytefish.pgbulkinsert.row.SimpleRow
import de.bytefish.pgbulkinsert.row.SimpleRowWriter
import io.vyne.cask.ingest.CaskEntityMutatedMessage
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
   fun write(rowWriter: SimpleRow, attributeSet: InstanceAttributeSet): CaskEntityMutatedMessage {
      columns.map { column ->
         val value = column.readValue(attributeSet)
         value?.run { column.write(rowWriter, value) }
      }
      return CaskEntityMutatedMessage(
         "caskNameGoesHere",
         "tableNameGoesHere",
         emptyList(), // TODO - Identity
         attributeSet
      )

   }

   fun upsert(template: JdbcTemplate, instance: InstanceAttributeSet): CaskEntityMutatedMessage {
      val upsertRowStatement: String = postgresDdlGenerator.generateUpsertDml(type, instance)
      template.execute(upsertRowStatement)
      return CaskEntityMutatedMessage(
         "caskNameGoesHere",
         "tableNameGoesHere",
         emptyList(), // TODO - Identity
         instance
      )
   }

   private val postgresDdlGenerator = PostgresDdlGenerator()
   val columns = PostgresDdlGenerator().generateDdl(type, schema).columns
   val tableName = PostgresDdlGenerator.tableName(type)
   val rowWriterTable = SimpleRowWriter.Table(tableName, *columns.map { it.name }.toTypedArray())
}
