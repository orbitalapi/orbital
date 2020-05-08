package io.vyne.cask.ingest

import com.google.common.annotations.VisibleForTesting
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.cask.ddl.TypeMigration
import io.vyne.cask.format.csv.CsvBinaryCacheStreamSource
import io.vyne.cask.query.CaskServiceSchemaGenerator
import io.vyne.cask.types.FieldAdded
import io.vyne.cask.types.FieldChanged
import io.vyne.cask.types.TypeDiffer
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.types.ObjectType
import org.springframework.jdbc.core.JdbcTemplate
import reactor.core.publisher.Flux

class DataSourceUpgrader(
   val schema: TaxiSchema,
   val strategy: UpgradeDataSourceSpec,
   val jdbcTemplate: JdbcTemplate) {
   val ingester: Ingester by lazy {
      val originalTypeSchema = strategy.source.schema
      val originalType = originalTypeSchema.versionedType(strategy.targetType.fullyQualifiedName.fqn())
      val targetType = strategy.targetType.taxiType as ObjectType // TODO : Handle non-object types
      val fieldsToMap = TypeDiffer().compareType(originalType.taxiType, targetType)
         .filter { it is FieldAdded || it is FieldChanged } // Ignore removals
         .map { targetType.field(it.fieldName) }
      val migration = TypeMigration(
         strategy.targetType,
         fieldsToMap,
         originalType
      )
      val readCachePath = strategy.source.readCachePath!!
      val ingestionStream = IngestionStream(
         strategy.targetType,
         TypeDbWrapper(strategy.targetType, schema, readCachePath, migration),
         CsvBinaryCacheStreamSource(readCachePath, migration, schema)
      )
      Ingester(jdbcTemplate, ingestionStream)
   }

   fun execute(): Flux<InstanceAttributeSet> {
      ingester.initialize()
      return ingester.ingest()
   }

   @VisibleForTesting
   fun destroy() {
      ingester.destroy()
   }
}
