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
import io.vyne.cask.ingest.IngesterFactory
import io.vyne.cask.ingest.IngestionErrorRepository
import io.vyne.cask.ingest.IngestionEventHandler
import io.vyne.cask.ingest.IngestionStream
import io.vyne.cask.ingest.StreamSource
import io.vyne.cask.upgrade.UpdatableSchemaProvider
import io.vyne.schemas.VersionedType
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.log
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import org.apache.commons.csv.CSVFormat
import org.apache.commons.io.IOUtils
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import java.io.File
import java.net.URI
import javax.sql.DataSource

@DataJpaTest(properties = ["spring.main.web-application-type=none"])
@RunWith(SpringRunner::class)
@AutoConfigureEmbeddedDatabase(beanName = "dataSource")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(StringToQualifiedNameConverter::class)
abstract class BaseCaskIntegrationTest {

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

   lateinit var schemaProvider: UpdatableSchemaProvider
   lateinit var viewDefinitions: MutableList<CaskViewDefinition>
   lateinit var caskViewService: CaskViewService
   lateinit var ingestionEventHandler: IngestionEventHandler

   @After
   fun tearDown() {
      configRepository.findAll().forEach {
         try {
            caskDao.deleteCask(it.tableName)
         } catch (e: Exception) {
            log().error("Failed to delete cask ${it.tableName}", e)
         }

      }
   }

   val taxiSchema: TaxiSchema
      get() {
         return schemaProvider.schema() as TaxiSchema
      }

   fun versionedType(name: String): VersionedType {
      return taxiSchema.versionedType(name.fqn())
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

      ingest(pipelineSource, versionedType, taxiSchema)
   }

   fun ingestJsonData(content: String, versionedType: VersionedType, taxiSchema: TaxiSchema, dropCaskFirst: Boolean = true) {
      val pipelineSource = JsonStreamSource(
         Flux.just(IOUtils.toInputStream(content)),
         versionedType,
         taxiSchema,
         MessageIds.uniqueId(),
         ObjectMapper())

      ingest(pipelineSource, versionedType, taxiSchema, dropCaskFirst)
   }


   fun ingestCsvData(resource: URI, versionedType: VersionedType, taxiSchema: TaxiSchema, dropCaskFirst: Boolean = true) {
      val pipelineSource = CsvStreamSource(
         Flux.just(File(resource).inputStream()),
         versionedType,
         taxiSchema,
         MessageIds.uniqueId(),
         csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader(),
         ingestionErrorProcessor = caskIngestionErrorProcessor)

      ingest(pipelineSource, versionedType, taxiSchema, dropCaskFirst)
   }

   fun ingest(source: StreamSource, versionedType: VersionedType, taxiSchema: TaxiSchema, dropCaskFirst: Boolean = true) {
      val pipeline = IngestionStream(
         versionedType,
         TypeDbWrapper(versionedType, taxiSchema),
         source)

      val ingester = IngesterFactory(jdbcTemplate, caskIngestionErrorProcessor)
         .create(pipeline)
      if (dropCaskFirst) {
         caskDao.dropCaskRecordTable(versionedType)
         caskDao.createCaskRecordTable(versionedType)
         caskConfigService.createCaskConfig(versionedType)
      }

      ingester.ingest().toList()
//         .doOnError { error ->
//            log().error("Error ", error)
//         }
//         .block(Duration.ofMillis(500))
   }


}
