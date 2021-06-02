package io.vyne.cask.upgrade

import com.google.common.io.Resources
import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import io.vyne.ParsedSource
import io.vyne.VersionedSource
import io.vyne.cask.CaskService
import io.vyne.cask.api.CaskStatus
import io.vyne.cask.api.CsvIngestionParameters
import io.vyne.cask.ddl.caskRecordTable
import io.vyne.cask.ddl.views.CaskViewDefinition
import io.vyne.cask.ddl.views.JoinExpression
import io.vyne.cask.ddl.views.ViewJoin
import io.vyne.cask.format.csv.CoinbaseOrderSchema
import io.vyne.cask.ingest.IngesterFactory
import io.vyne.cask.query.BaseCaskIntegrationTest
import io.vyne.cask.websocket.CsvWebsocketRequest
import io.vyne.schemaStore.SchemaSet
import io.vyne.schemaStore.SchemaSourceProvider
import io.vyne.schemaStore.SchemaStore
import io.vyne.schemas.Schema
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.types.QualifiedName
import org.junit.Before
import org.junit.Test
import reactor.core.publisher.Flux
import java.time.Duration

// Note : These tests must not be transactional, as the create tables
// happen outside of the transaction, meaning they can't be seen.
class CaskUpgraderServiceIntegrationTest : BaseCaskIntegrationTest() {

   lateinit var changeDetector: CaskSchemaChangeDetector
   lateinit var caskUpgrader: CaskUpgraderService

   lateinit var caskService: CaskService


   @Before
   override fun setup() {
      super.setup()
      val ingestorFactory = IngesterFactory(jdbcTemplate, caskIngestionErrorProcessor)
      changeDetector = CaskSchemaChangeDetector(configRepository, caskConfigService, caskDao, caskViewService)
      caskUpgrader = CaskUpgraderService(caskDao, schemaProvider, ingestorFactory, configRepository, applicationEventPublisher = mock { }, caskIngestionErrorProcessor = caskIngestionErrorProcessor)
      caskService = CaskService(schemaProvider, ingestorFactory, configRepository, caskDao, ingestionErrorRepository, caskViewService, mock {  }, mock {  })
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
         versionedType,
         caskIngestionErrorProcessor
      ), Flux.just(source.openStream())).blockLast(Duration.ofSeconds(2L))

      val originalRecords = caskDao.findAll(versionedType)
      originalRecords.should.have.size(10)
      val originalRecord = originalRecords.first()
      originalRecord.should.have.keys("symbol", "open", "close", "caskmessageid", "cask_raw_id")
      originalRecord.keys.should.have.size(5)

      schemaProvider.updateSource(CoinbaseOrderSchema.sourceV2)
      // Now trigger a migration to the next schema version
      val tablesToMigrate = changeDetector.markModifiedCasksAsRequiringUpgrading(CoinbaseOrderSchema.schemaV2)
      tablesToMigrate.should.have.size(1)

      val caskNeedingUpgrade = tablesToMigrate[0]
      caskUpgrader.upgrade(caskNeedingUpgrade.config)

      val upgradedRecords = caskDao.findAll(tablesToMigrate.first().newType.caskRecordTable())
      upgradedRecords.should.have.size(10)

      val upgradedRecord = upgradedRecords.first()
      upgradedRecord.should.have.keys("symbol", "open", "high", "close", "caskmessageid", "cask_raw_id")

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
         versionedType,
         caskIngestionErrorProcessor
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

   @Test
   fun `can migrate cask with dependent view`() {
      // Before we start, lets define the view.
      // This means that the view should get created automatically once both of the underlying
      // casks are present.
      // It's also required because the caskViewService expects the set of views to be
      // defined immutably through config at startup.
      val schema = schemaProvider.updateSource(CoinbaseOrderSchema.personAndOrderSourceV1)
      val orderType = schema.versionedType("coinbase.OrderWindowSummary".fqn())
      val personType = schema.versionedType("demo.Person".fqn())
      viewDefinitions.add(CaskViewDefinition(
         QualifiedName.from("OrderWithPerson"),
         join = ViewJoin(
            kind = ViewJoin.ViewJoinKind.INNER,
            left = QualifiedName.from(personType.fullyQualifiedName),
            right = QualifiedName.from(orderType.fullyQualifiedName),
            // Note: THis join doesn't generate any rows, but that's not the purpose of this test
            joinOn = listOf(JoinExpression("id", "symbol"))
         )
      ))
      // At this stage, creation of the view should fail, as the underlying tables don't exist
      caskViewService.generateViews().should.be.empty


      // First, create a table with the original schema
      val personSource = Resources.getResource("Person_date_time.csv")
      caskDao.createCaskRecordTable(personType)
      caskConfigService.createCaskConfig(personType)
      caskService.ingestRequest(CsvWebsocketRequest(
         CsvIngestionParameters(),
         personType,
         caskIngestionErrorProcessor
      ), Flux.just(personSource.openStream())).blockLast(Duration.ofSeconds(2L))

      // First, create a table with the original schema
      val orderSource = Resources.getResource("Coinbase_BTCUSD_10_records.csv")
      caskDao.createCaskRecordTable(orderType)
      caskConfigService.createCaskConfig(orderType)
      caskService.ingestRequest(CsvWebsocketRequest(
         CsvIngestionParameters(),
         orderType,
         caskIngestionErrorProcessor
      ), Flux.just(orderSource.openStream())).blockLast(Duration.ofSeconds(2L))

      // Now, creation of the views should succeed
      caskViewService.generateViews().should.have.size(1)

      // At this point, there should be 3 casks present - the two base casks, and the view
      val allConfigs = configRepository.findAll()
      allConfigs.should.have.size(3)
      configRepository.findAllByQualifiedTypeName("OrderWithPerson").should.have.size(1)

      val count = jdbcTemplate.queryForObject("select count(*) from v_OrderWithPerson", Int::class.java)
      count.should.equal(0)


      // Now change the schema of one of the views
      val updatedSchema = schemaProvider.updateSource(CoinbaseOrderSchema.personV2AndOrderSourceV1)
      // Now trigger a migration to the next schema version
      val tablesToMigrate = changeDetector.markModifiedCasksAsRequiringUpgrading(updatedSchema)
      tablesToMigrate.should.have.size(1)

      // At this point, the view should've been deleted
      configRepository.findAllByQualifiedTypeName("OrderWithPerson").should.be.empty
      try {
         jdbcTemplate.queryForObject("select count(*) from v_OrderWithPerson", Int::class.java)
      } catch (exception:Exception) {
         // View shouldn't exist anymore
         exception.message.should.contain("""relation "v_orderwithperson" does not exist""")
      }

      caskUpgrader.upgradeAll(tablesToMigrate)

      caskViewService.generateViews().should.have.size(1)
   }
}

class UpdatableSchemaProvider : SchemaSourceProvider, SchemaStore {
   companion object {
      fun withSource(source: String): UpdatableSchemaProvider {
         return UpdatableSchemaProvider().apply {
            updateSource(source)
         }
      }
   }

   override var generation: Int = 0
   private lateinit var source: String
   private lateinit var schema: TaxiSchema
   private lateinit var schemaSet: SchemaSet
   fun updateSource(source: String): TaxiSchema {
      this.source = source
      this.schema = TaxiSchema.from(source)
      generation++
      this.schemaSet = SchemaSet.fromParsed(listOf(ParsedSource(VersionedSource.sourceOnly(source))), generation)
      return this.schema
   }

   override fun schemas(): List<Schema> {
      return listOf(schema)
   }

   override fun schemaStrings(): List<String> {
      return listOf(source)
   }

   override fun schemaSet(): SchemaSet {
      return schemaSet
   }


}
