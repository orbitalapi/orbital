package io.vyne.cask.ddl

import de.bytefish.pgbulkinsert.row.SimpleRow
import de.bytefish.pgbulkinsert.row.SimpleRowWriter
import io.vyne.cask.ingest.CaskEntityMutatingMessage
import io.vyne.cask.ingest.CaskIdColumnValue
import io.vyne.cask.ingest.InstanceAttributeSet
import io.vyne.cask.ingest.TaxiAnnotationHelper
import io.vyne.schemas.Schema
import io.vyne.schemas.VersionedType
import lang.taxi.types.Field
import lang.taxi.types.ObjectType
import mu.KotlinLogging
import org.springframework.jdbc.core.JdbcTemplate

data class TypeMigration(
   val targetType: VersionedType,
   val fields: List<Field>,
   val predecessorType: VersionedType
)
private val logger = KotlinLogging.logger {}
class TypeDbWrapper(val type: VersionedType, schema: Schema) {
   private val postgresDdlGenerator = PostgresDdlGenerator()
   val columns = postgresDdlGenerator.generateDdl(type, schema).columns
   val tableName = PostgresDdlGenerator.tableName(type)
   val rowWriterTable = SimpleRowWriter.Table(tableName, *columns.map { it.name }.toTypedArray())
   private val writeToConnectionName = TaxiAnnotationHelper.observeChangesConnectionName(type.taxiType as ObjectType)

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
         attributeSet,
         null,
         writeToConnectionName
      )
   }

   fun upsert(template: JdbcTemplate, instance: InstanceAttributeSet): CaskEntityMutatingMessage {
      val (upsertInformation, idColumnValues) = postgresDdlGenerator.generateUpsertDml(type, instance, writeToConnectionName != null)
      val oldValues = if (writeToConnectionName != null) {
         // if we're observing the changes for this type, we need to fetch the values before the update.
         val returnValues = template.queryForList(upsertInformation)
         if (returnValues.size != 1) {
            logger.info { "Unexpected result set size when fetching old values for an upsert, type name is ${type.fullyQualifiedName}" }
            null
         } else {
            returnValues.first()
         }
      } else {
         template.execute(upsertInformation)
         null
      }

      return CaskEntityMutatingMessage(
         tableName,
         idColumnValues,
         instance,
         oldValues,
         writeToConnectionName
      )
   }
}
