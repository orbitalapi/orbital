package io.vyne.cask.ingest


// Storing this here for historic purposes,
// should we choose to go back to the upgrad-column-at-a-time approach

//class DataSourceUpgrader(
//   val schema: TaxiSchema,
//   val strategy: UpgradeDataSourceSpec,
//   val jdbcTemplate: JdbcTemplate) {
//   val ingester: Ingester by lazy {
//      val originalTypeSchema = strategy.source.schema
//      val originalType = originalTypeSchema.versionedType(strategy.targetType.fullyQualifiedName.fqn())
//      val targetType = strategy.targetType.taxiType as ObjectType // TODO : Handle non-object types
//      val fieldsToMap = TypeDiffer().compareType(originalType.taxiType, targetType)
//         .filter { it is FieldAdded || it is FieldChanged } // Ignore removals
//         .map { targetType.field(it.fieldName) }
//      val migration = TypeMigration(
//         strategy.targetType,
//         fieldsToMap,
//         originalType
//      )
//      val readCachePath = strategy.source.readCachePath!!
//      val ingestionStream = IngestionStream(
//         strategy.targetType,
//         TypeDbWrapper(strategy.targetType, schema, readCachePath, migration),
//         CsvBinaryCacheStreamSource(readCachePath, migration, schema)
//      )
//      Ingester(jdbcTemplate, ingestionStream)
//   }
//
//   fun execute(): Flux<InstanceAttributeSet> {
//      ingester.initialize()
//      return ingester.ingest()
//   }
//
//   @VisibleForTesting
//   fun destroy() {
//      ingester.destroy()
//   }
//}
