package io.vyne.cask.query

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.cask.MessageIds
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.config.StringToQualifiedNameConverter
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.cask.ddl.views.CaskViewBuilderFactory
import io.vyne.cask.ddl.views.CaskViewConfig
import io.vyne.cask.ddl.views.CaskViewDefinition
import io.vyne.cask.ddl.views.CaskViewService
import io.vyne.cask.format.csv.CsvStreamSource
import io.vyne.cask.format.json.CoinbaseJsonOrderSchema
import io.vyne.cask.format.json.JsonStreamSource
import io.vyne.cask.ingest.CaskIngestionErrorProcessor
import io.vyne.cask.ingest.CaskMessageRepository
import io.vyne.cask.ingest.Ingester
import io.vyne.cask.ingest.IngestionError
import io.vyne.cask.ingest.IngestionErrorRepository
import io.vyne.cask.ingest.IngestionEventHandler
import io.vyne.cask.ingest.IngestionStream
import io.vyne.cask.upgrade.UpdatableSchemaProvider
import io.vyne.schemas.VersionedType
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.spring.SimpleTaxiSchemaProvider
import io.vyne.utils.log
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import org.apache.commons.csv.CSVFormat
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.UnicastProcessor
import java.io.File
import java.lang.Exception
import java.net.URI
import java.time.Duration
import javax.sql.DataSource

@DataJpaTest(properties = ["spring.main.web-application-type=none"])
@RunWith(SpringRunner::class)
@AutoConfigureEmbeddedDatabase(beanName = "dataSource")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(StringToQualifiedNameConverter::class)
abstract class BaseCaskIntegrationTest  {

   @Autowired
   lateinit var configRepository: CaskConfigRepository

   @Autowired
   lateinit var caskMessageRepository: CaskMessageRepository

   @Autowired
   lateinit var dataSource: DataSource

   @Autowired
   lateinit var jdbcTemplate: JdbcTemplate
   @Autowired
   lateinit var ingestionErrorRepository: IngestionErrorRepository
   lateinit var caskIngestionErrorProcessor: CaskIngestionErrorProcessor
   lateinit var caskDao: CaskDAO
   lateinit var caskConfigService: CaskConfigService

   lateinit var schemaProvider:UpdatableSchemaProvider
   lateinit var viewDefinitions: MutableList<CaskViewDefinition>
   lateinit var caskViewService: CaskViewService
   lateinit var ingestionEventHandler: IngestionEventHandler

   @After
   fun tearDown() {
      configRepository.findAll().forEach {
         try {
            caskDao.deleteCask(it.tableName)
         } catch (e:Exception) {
            log().error("Failed to delete cask ${it.tableName}",e)
         }

      }

   }
   @Before
   fun setup() {
      caskIngestionErrorProcessor = CaskIngestionErrorProcessor(ingestionErrorRepository)
      schemaProvider = UpdatableSchemaProvider.withSource(CoinbaseJsonOrderSchema.sourceV1)
      caskDao = CaskDAO(jdbcTemplate, schemaProvider, dataSource, caskMessageRepository, configRepository)
      caskConfigService = CaskConfigService(configRepository)
      viewDefinitions = mutableListOf()
      caskViewService = CaskViewService(
         CaskViewBuilderFactory(configRepository, schemaProvider),
         configRepository,
         jdbcTemplate,
         CaskViewConfig(viewDefinitions)
      )
      ingestionEventHandler = IngestionEventHandler(caskConfigService, caskDao)
   }

   fun ingestJsonData(resource: URI, versionedType: VersionedType, taxiSchema: TaxiSchema) {
      val pipelineSource = JsonStreamSource(
         Flux.just(File(resource).inputStream()),
         versionedType,
         taxiSchema,
         MessageIds.uniqueId(),
         ObjectMapper())

      val pipeline = IngestionStream(
         versionedType,
         TypeDbWrapper(versionedType, taxiSchema),
         pipelineSource)

      val ingester = Ingester(jdbcTemplate, pipeline, UnicastProcessor.create<IngestionError>().sink())
      caskDao.dropCaskRecordTable(versionedType)
      caskDao.createCaskRecordTable(versionedType)
      caskConfigService.createCaskConfig(versionedType)

      ingester.ingest().collectList()
         .doOnError { error ->
            log().error("Error ", error)
         }
         .block(Duration.ofMillis(500))
   }

   fun ingestCsvData(resource: URI, versionedType: VersionedType, taxiSchema: TaxiSchema) {
      val pipelineSource = CsvStreamSource(
         Flux.just(File(resource).inputStream()),
         versionedType,
         taxiSchema,
         MessageIds.uniqueId(),
         csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader(),
         ingestionErrorProcessor = caskIngestionErrorProcessor)

      val pipeline = IngestionStream(
         versionedType,
         TypeDbWrapper(versionedType, taxiSchema),
         pipelineSource)

      val ingester = Ingester(jdbcTemplate, pipeline, UnicastProcessor.create<IngestionError>().sink())
      caskDao.dropCaskRecordTable(versionedType)
      caskDao.createCaskRecordTable(versionedType)
      caskConfigService.createCaskConfig(versionedType)

      ingester.ingest().collectList()
         .doOnError { error ->
            log().error("Error ", error)
         }
         .block(Duration.ofMillis(500))
   }


}
