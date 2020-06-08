package io.vyne.cask.query

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.io.Resources
import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import com.winterbe.expekt.should
import io.vyne.cask.ddl.TableMetadata
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.cask.format.json.CoinbaseJsonOrderSchema
import io.vyne.cask.format.json.JsonStreamSource
import io.vyne.cask.ingest.Ingester
import io.vyne.cask.ingest.IngestionStream
import io.vyne.schemas.fqn
import io.vyne.spring.SimpleTaxiSchemaProvider
import io.vyne.utils.log
import org.apache.commons.io.FileUtils
import org.flywaydb.core.Flyway
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.jdbc.core.JdbcTemplate
import reactor.core.publisher.Flux
import java.io.File
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.util.*

@Ignore
class CaskDAOIntegrationTest {

   @Rule
   @JvmField
   val folder = TemporaryFolder()

   @Rule
   @JvmField
   val pg = EmbeddedPostgresRules.singleInstance().customize { it.setPort(0) }

   lateinit var jdbcTemplate: JdbcTemplate
   lateinit var caskDao: CaskDAO

   @Before
   fun setup() {
      val dataSource = DataSourceBuilder.create()
         .url("jdbc:postgresql://localhost:${pg.embeddedPostgres.port}/postgres")
         .username("postgres")
         .build()
      Flyway.configure()
         .dataSource(dataSource)
         .load()
         .migrate()
      jdbcTemplate = JdbcTemplate(dataSource)
      jdbcTemplate.execute(TableMetadata.DROP_TABLE)
      caskDao = CaskDAO(jdbcTemplate, SimpleTaxiSchemaProvider(CoinbaseJsonOrderSchema.sourceV1))
   }

   @Test
   fun canQueryIngestedDataFromDatabase() {
      val taxiSchema = CoinbaseJsonOrderSchema.schemaV1
      val versionedType = taxiSchema.versionedType("OrderWindowSummary".fqn())
      val resource = Resources.getResource("Coinbase_BTCUSD.json").toURI()

      val pipelineSource = JsonStreamSource(
         Flux.just(File(resource).inputStream()),
         versionedType,
         taxiSchema,
         folder.root.toPath(),
         ObjectMapper())

      val pipeline = IngestionStream(
         versionedType,
         TypeDbWrapper(versionedType, taxiSchema, pipelineSource.cachePath, null),
         pipelineSource)

      val ingester = Ingester(jdbcTemplate, pipeline)
      caskDao.dropCaskRecordTable(versionedType)
      caskDao.createCaskRecordTable(versionedType)

      ingester.ingest().collectList()
         .doOnError { error ->
            log().error("Error ", error)
         }
         .block(Duration.ofMillis(500))

      caskDao.findBy(versionedType, "symbol", "BTCUSD").size.should.equal(10061)
      caskDao.findBy(versionedType, "open", "6300").size.should.equal(7)
      caskDao.findBy(versionedType, "close", "6330").size.should.equal(9689)
      caskDao.findBy(versionedType, "orderDate", "2020-03-19").size.should.equal(10061)
      FileUtils.cleanDirectory(folder.root)
   }

   @Test
   fun canCreateCaskRecordTable() {
      // prepare
      val taxiSchema = CoinbaseJsonOrderSchema.schemaV1
      val versionedType = taxiSchema.versionedType("OrderWindowSummary".fqn())
      caskDao.dropCaskRecordTable(versionedType)

      // act
      val tableName = caskDao.createCaskRecordTable(versionedType)

      // assert we can insert to the new cask table
      jdbcTemplate.execute("""insert into $tableName(symbol, open, close, "orderDate") values ('BTCUSD', '6300', '6330', '2020-03-19')""")
   }

   @Test
   fun canCreateCaskConfig() {
      // prepare
      val taxiSchema = CoinbaseJsonOrderSchema.schemaV1
      val versionedType = taxiSchema.versionedType("OrderWindowSummary".fqn())

      // act
      caskDao.createCaskConfig(versionedType)
      caskDao.createCaskConfig(versionedType)

      // assert
      val caskConfigs: MutableList<CaskDAO.CaskConfig> = caskDao.findCaskConfigByType(versionedType)
      caskConfigs.size.should.be.equal(1)
      caskConfigs[0].tableName.should.equal("rderWindowSummary_f1b588_1b7171")
      caskConfigs[0].qualifiedTypeName.should.equal("OrderWindowSummary")
      caskConfigs[0].versionHash.should.equal("1b7171")
      caskConfigs[0].sourceSchemaIds.should.contain.elements("Coinbase:0.1.0")
      caskConfigs[0].sources.should.contain.elements(CoinbaseJsonOrderSchema.sourceV1)
      caskConfigs[0].deltaAgainstTableName.should.be.`null`
      caskConfigs[0].insertedAt.should.be.below(Instant.now())
   }

   @Test
   fun canAddCaskMessage() {
      // prepare
      val taxiSchema = CoinbaseJsonOrderSchema.schemaV1
      val versionedType = taxiSchema.versionedType("OrderWindowSummary".fqn())
      val messageId = UUID.randomUUID().toString()
      val messagePath = Paths.get("/caks/messages/OrderWindowSummary/${messageId}")

      // act
      caskDao.createCaskMessage(versionedType, messagePath, messageId)

      // assert
      val caskMessages: MutableList<CaskDAO.CaskMessage> = caskDao.findAllCaskMessages()
      caskMessages.size.should.be.equal(1)
      caskMessages[0].id.should.not.be.empty
      caskMessages[0].readCachePath.should.equal("OrderWindowSummary")
      caskMessages[0].qualifiedTypeName.should.equal(messagePath.toString())
      caskMessages[0].insertedAt.should.be.below(Instant.now())
   }
}
