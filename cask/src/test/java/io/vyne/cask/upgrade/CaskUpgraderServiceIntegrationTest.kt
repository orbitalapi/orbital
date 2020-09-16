package io.vyne.cask.upgrade

import com.google.common.io.Resources
import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import io.vyne.cask.CaskService
import io.vyne.cask.api.CaskStatus
import io.vyne.cask.api.CsvIngestionParameters
import io.vyne.cask.ddl.caskRecordTable
import io.vyne.cask.format.csv.CoinbaseOrderSchema
import io.vyne.cask.ingest.IngesterFactory
import io.vyne.cask.query.BaseCaskIntegrationTest
import io.vyne.cask.websocket.CsvWebsocketRequest
import io.vyne.schemaStore.SchemaSourceProvider
import io.vyne.schemas.Schema
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Before
import org.junit.Test
import reactor.core.publisher.Flux
import java.time.Duration

// Note : These tests must not be transactional, as the create tables
// happen outside of the transaction, meaning they can't be seen.
class CaskUpgraderServiceIntegrationTest : BaseCaskIntegrationTest() {

   lateinit var changeDetector: CaskSchemaChangeDetector
   lateinit var caskUpgrader: CaskUpgraderService

   lateinit var caskService:CaskService

   @Before
   override fun setup() {
      super.setup()
      val ingestorFactory = IngesterFactory(jdbcTemplate)
      changeDetector = CaskSchemaChangeDetector(configRepository, caskConfigService, caskDao)
      caskUpgrader = CaskUpgraderService(caskDao, schemaProvider, ingestorFactory, configRepository, applicationEventPublisher =  mock {  })
      caskService = CaskService(schemaProvider,ingestorFactory,configRepository,caskDao)
   }

   @Test
   fun ingestedRowsAreMigratedToNewTable() {
      // First, create a table with the original schema
      val source = Resources.getResource("Coinbase_BTCUSD_10_records.csv")
      schemaProvider.updateSource(CoinbaseOrderSchema.sourceV1)
      val versionedType = CoinbaseOrderSchema.schemaV1.versionedType("OrderWindowSummary".fqn())
      caskDao.createCaskRecordTable(versionedType)
      caskConfigService.createCaskConfig(versionedType)

      // Ingest the data
      // We're using the service to ensure the message record is created
      caskService.ingestRequest(CsvWebsocketRequest(
         CsvIngestionParameters(),
         versionedType
      ), Flux.just(source.openStream())).blockLast(Duration.ofSeconds(2L))

      val originalRecords = caskDao.findAll(versionedType)
      originalRecords.should.have.size(10)
      val originalRecord = originalRecords.first()
      originalRecord.should.have.keys("symbol","open","close","caskmessageid")
      originalRecord.keys.should.have.size(4)

      schemaProvider.updateSource(CoinbaseOrderSchema.sourceV2)
      // Now trigger a migration to the next schema version
      val tablesToMigrate = changeDetector.markModifiedCasksAsRequiringUpgrading(CoinbaseOrderSchema.schemaV2)
      tablesToMigrate.should.have.size(1)

      val caskNeedingUpgrade = tablesToMigrate[0]
      caskUpgrader.upgrade(caskNeedingUpgrade.config)

      val upgradedRecords = caskDao.findAll(tablesToMigrate.first().newType.caskRecordTable())
      upgradedRecords.should.have.size(10)

      val upgradedRecord = upgradedRecords.first()
      upgradedRecord.should.have.keys("symbol","open","high","close","caskmessageid")

      val deprecatedCask = configRepository.findByTableName(caskNeedingUpgrade.config.tableName)!!
      deprecatedCask.status.should.equal(CaskStatus.REPLACED)
   }

   @Test // This test adds columns with pk's and annotations
   fun canMigrateTableWithPrimaryKey() {
      // First, create a table with the original schema
      val source = Resources.getResource("Person_date_time.csv")
      schemaProvider.updateSource(CoinbaseOrderSchema.personSourceV1)
      val versionedType = CoinbaseOrderSchema.personSchemaV1.versionedType("demo.Person".fqn())
      caskDao.createCaskRecordTable(versionedType)
      caskConfigService.createCaskConfig(versionedType)

      caskService.ingestRequest(CsvWebsocketRequest(
         CsvIngestionParameters(),
         versionedType
      ), Flux.just(source.openStream())).blockLast(Duration.ofSeconds(2L))

      schemaProvider.updateSource(CoinbaseOrderSchema.personSourceV2)
      // Now trigger a migration to the next schema version
      val tablesToMigrate = changeDetector.markModifiedCasksAsRequiringUpgrading(CoinbaseOrderSchema.personSchemaV2)
      tablesToMigrate.should.have.size(1)

      val caskNeedingUpgrade = tablesToMigrate[0]
      caskUpgrader.upgrade(caskNeedingUpgrade.config)

      val upgradedRecords = caskDao.findAll(tablesToMigrate.first().newType.caskRecordTable())
      upgradedRecords.should.have.size(4)

      upgradedRecords.first().should.have.keys(
         "id",
         "firstName",
         "lastName",
         "logDate",
         "logTime",
         "caskmessageid"
      )

   }
}

class UpdatableSchemaProvider : SchemaSourceProvider {
   companion object {
      fun withSource(source:String) : UpdatableSchemaProvider {
         return UpdatableSchemaProvider().apply {
            updateSource(source)
         }
      }
   }
   private lateinit var source: String
   private lateinit var schema: TaxiSchema
   fun updateSource(source: String):TaxiSchema {
      this.source = source
      this.schema = TaxiSchema.from(source)
      return this.schema
   }

   override fun schemas(): List<Schema> {
      return listOf(schema)
   }

   override fun schemaStrings(): List<String> {
      return listOf(source)
   }

}
