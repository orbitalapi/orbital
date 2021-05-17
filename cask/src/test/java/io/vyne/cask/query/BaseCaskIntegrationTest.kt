package io.vyne.cask.query

import com.fasterxml.jackson.databind.ObjectMapper
import com.zaxxer.hikari.HikariDataSource
import io.vyne.cask.MessageIds
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.config.JdbcStreamingTemplate
import io.vyne.cask.config.StringToQualifiedNameConverter
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.cask.ddl.views.*
import io.vyne.cask.ddl.views.CaskViewBuilderFactory
import io.vyne.cask.ddl.views.CaskViewConfig
import io.vyne.cask.ddl.views.CaskViewDefinition
import io.vyne.cask.ddl.views.CaskViewService
import io.vyne.cask.ddl.views.taxiViews.SchemaBasedViewGenerator
import io.vyne.cask.format.csv.CsvStreamSource
import io.vyne.cask.format.json.CoinbaseJsonOrderSchema
import io.vyne.cask.format.json.JsonStreamSource
import io.vyne.cask.ingest.*
import io.vyne.cask.upgrade.UpdatableSchemaProvider
import io.vyne.schemas.VersionedType
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.log
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import io.zonky.test.db.flyway.BlockingDataSourceWrapper
import org.apache.commons.csv.CSVFormat
import org.apache.commons.io.IOUtils
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanPostProcessor
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
import java.io.PrintWriter
import java.net.URI
import java.sql.Connection
import java.time.Duration
import java.util.logging.Logger
import javax.sql.DataSource

@DataJpaTest(properties = ["spring.main.web-application-type=none"])
@RunWith(SpringRunner::class)
@AutoConfigureEmbeddedDatabase(beanName = "dataSource")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(StringToQualifiedNameConverter::class, JdbcStreamingTemplate::class, PostProcessorConfiguration::class)
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
   lateinit var jdbcStreamingTemplate: JdbcStreamingTemplate


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
            caskDao.deleteCask(it)
         } catch (e: Exception) {
            log().error("Failed to delete cask ${it.tableName}", e)
         }

      }
   }

   val connectionCountingDataSource: ConnectionCountingDataSource
      get() {
         return dataSource as ConnectionCountingDataSource
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
         CaskViewBuilderFactory(configRepository, schemaProvider, StringToQualifiedNameConverter()),
         configRepository,
         jdbcTemplate,
         CaskViewConfig(viewDefinitions),
         SchemaBasedViewGenerator(configRepository, schemaProvider)
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

      val ingester = Ingester(jdbcTemplate, pipeline, UnicastProcessor.create<IngestionError>().sink())
      if (dropCaskFirst) {
         caskDao.dropCaskRecordTable(versionedType)
         caskDao.createCaskRecordTable(versionedType)
         caskConfigService.createCaskConfig(versionedType)
      }

      ingester.ingest().collectList()
         .doOnError { error ->
            log().error("Error ", error)
         }
         .block(Duration.ofMillis(500))
   }


}

@Configuration
class PostProcessorConfiguration {
   @Bean
   fun eventBusBeanPostProcessor(): DataSourceBeanPostProcessor {
      return DataSourceBeanPostProcessor()
   }

}

class DataSourceBeanPostProcessor: BeanPostProcessor {
   override fun postProcessAfterInitialization(bean: Any, beanName: String): Any? {
      if (bean is DataSource) {
         return ConnectionCountingDataSource(bean)
      }
      return super.postProcessAfterInitialization(bean, beanName)
   }

}

class ConnectionCountingDataSource(val dataSource: DataSource): DataSource by dataSource {
   val connectionList: MutableList<Connection> = mutableListOf()
   override fun getConnection(): Connection {
      val connection =  dataSource.connection
      connectionList.add(connection)
      return  connection
   }

   override fun getConnection(username: String?, password: String?): Connection {
     val connection = dataSource.getConnection(username, password)
      connectionList.add(connection)
      return connection
   }

}
