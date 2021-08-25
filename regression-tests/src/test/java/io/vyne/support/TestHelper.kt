package io.vyne.support

import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.TaxonomyRegressionTest
import io.vyne.testcontainers.CommonSettings
import io.vyne.testcontainers.MonitoringSystem
import io.vyne.testcontainers.VyneSystem
import mu.KotlinLogging
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.springframework.core.io.ClassPathResource
import org.testcontainers.containers.KafkaContainer
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

private val logger = KotlinLogging.logger {}
object TestHelper {
   const val londonOrderType = "bank.london.Order"
   private lateinit var vyneSystem: VyneSystem
   private lateinit var kafka: KafkaContainer
   private lateinit var monitoringSystem: MonitoringSystem
   const val niceaType = "nicea.rfq.RfqCbIngestion"
   const val troyType = "troy.orders.Order"
   const val smyrnaType = "smyrna.orders.Order"
   const val persepolisType = "persepolis.orders.Order"
   const val philadelphiaType = "philadelphia.orders.Order"
   const val magnesiaType = "magnesia.priceQuote.PriceQuote"
   const val knidosType = "knidos.orders.Order"
   const val tenedosOrdersType = "tenedos.orders.Order"
   const val tenedosTradesType = "tenedos.trade.Trade"
   const val rfqConvertibleBondsType = "bank.rfq.RfqConvertibleBond"
   const val rfqIrdType = "bank.rfq.RfqIrdIngestion"
   const val bankOrdersType = "bank.orders.Order"
   const val bristolCBOrderReportType = "bank.bristol.CBOrderReport"
   const val bankRfqType = "bank.rfq.Rfq"
   const val rfqConvertibleBondsReport = "bank.rfq.RfqConvertibleBondsReport"
   const val bankIrdReport = "bank.london.RfqIrdReport"
   const val lesbosWebOrderSent="lesbos.orders.OrderSent"
   const val lesbosWebOrderFilled="lesbos.orders.OrderFilled"
   const val lesbosWebOrderReportView = "lesbos.orders.DWOrderReportView"
   private var runMode: RunMode = RunMode.Local

   val csvFormat: CSVFormat = CSVFormat.DEFAULT
      .withIgnoreEmptyLines()
      .withIgnoreSurroundingSpaces()
      .withFirstRecordAsHeader()
      .withAllowMissingColumnNames()
      .withAllowDuplicateHeaderNames()

   fun init() {
      runMode =  when(System.getProperty("run.mode")) {
         "docker" -> RunMode.Docker
         "local" -> RunMode.Local
         else -> runMode
      }

      if (runMode == RunMode.Local) {
         return
      }

      val tenedosPipelineDefinition = TaxonomyRegressionTest::class.java.getResourceAsStream("/tenedos-order-pipeline-definition.json")
      val vyneDockerTag =  if (System.getProperty("vyne.tag") == null) CommonSettings.latestSnapshot else System.getProperty("vyne.tag")

      logger.warn("RUNNING TEST SUITE AGAINST VYNE DOCKER TAGS: $vyneDockerTag")
      val taxonomyPath = File("taxonomy/")
      vyneSystem = VyneSystem.withEurekaAndFileBasedSchema(taxonomyPath.absolutePath, vyneDockerTag, true)
      monitoringSystem = VyneSystem.monitoringSystem(vyneSystem)
      kafka = KafkaInitialiser.initialiseKafka(vyneSystem.network)
      kafka.start()
      vyneSystem.start()
      monitoringSystem.start()

      vyneSystem.vyneQueryServer.ensureType(londonOrderType)
      vyneSystem.ensurePipelineRunner()
      createPipeline(tenedosPipelineDefinition)
      vyneSystem.isPipelineRunning(10, 60000L)
      loadTenedosOrdersThroughPipelineRunner(kafka.bootstrapServers)
      postStaticDataFiles()
      loadBrokerFiles()
      vyneSystem.vyneQueryServer.ensureService("vyne.cask.${lesbosWebOrderReportView}CaskService")
   }
   fun destroy() {
      if (runMode == RunMode.Docker) {
         vyneSystem.close()
         monitoringSystem.close()
         kafka.close()
      }
   }

   fun submitVyneQl(vyneQl: String): String {
      return if (runMode == RunMode.Local) {
         val vyneEndPoint = if (System.getProperty("vyne.host") == null) {
            "localhost:9022"
         } else {
            System.getProperty("vyne.host")
         }
         submitToVyneQlToQueryServer(vyneEndPoint, vyneQl)
      } else {
         vyneSystem.vyneQueryServer.submitVyneQl(vyneQl)
      }
   }

   fun compareCsvContents(actual: String, expectedResponseFile: File) {
      val expectedCsv = CSVParser.parse(expectedResponseFile,
         Charset.defaultCharset(),
         csvFormat)
      val actualCsv = CSVParser.parse(actual, csvFormat)
      val expectedRecords = expectedCsv.records
      val actualRecords = actualCsv.records
      expect(actualRecords.size).to.equal(expectedRecords.size)
      val sortedExpectedRecords = expectedRecords.sortedBy { csvRecord -> csvRecord.joinToString() }
      val sortedActualRecords = actualRecords.sortedBy { csvRecord -> csvRecord.joinToString() }
      sortedActualRecords.forEachIndexed { index, csvRecord ->
         csvRecord.toList().should.equal(sortedExpectedRecords[index].toList())
      }
   }

   fun findAllExpectedFile(type: String) = File("src/test/resources/expected_responses/findAll/$type")

   private fun postStaticDataFiles() {
      val staticDataFilesPath = ClassPathResource("static").file
      Files.walk(Paths.get(staticDataFilesPath.absolutePath))
         .filter { Files.isRegularFile(it) }
         .filter {  it.toFile().extension == "csv" }
         .map { Pair(it.toFile().nameWithoutExtension, it.toFile()) }
         .collect(Collectors.toList())
         .forEach { (typeName, file) ->
            logger.info { "loading ${file.absolutePath} for type $typeName" }
            vyneSystem
               .vyneQueryServer
               .postCsvDataFile(file)

            vyneSystem.vyneQueryServer.ensureService("vyne.cask.${typeName}CaskService")
         }
   }

   private fun createPipeline(pipelineDefinitionJson: InputStream) {
      val request = Request
         .post("http://localhost:${vyneSystem.pipelineOrchestrator.firstMappedPort}/api/pipelines")
         .bodyStream(pipelineDefinitionJson)
         .setHeader("Accept", "application/json, text/javascript, */*")
         .setHeader("Content-Type", "application/json")

      val response = request.execute()
      response.handleResponse {
         if (it.code != 200) {
            val content = it.entity.content.reader().readText()
         }
      }
   }
   private fun loadTenedosOrdersThroughPipelineRunner(kafkaBootstrapServers: String) {
      val kafkaPublisher = KafkaPublisher(kafkaBootstrapServers, "pipeline-input-tenedos-order")
      val tenedosOrders = File("src/test/resources/tenedos/orders")
      Files.walk(Paths.get(tenedosOrders.absolutePath))
         .filter { Files.isRegularFile(it) }
         .forEach { ionOrderFile ->
            kafkaPublisher.publish(ionOrderFile.toFile())
         }

      vyneSystem.vyneQueryServer.ensureService("vyne.cask.${tenedosOrdersType}CaskService")
   }

   private fun loadBrokerFiles() {
      uploadCsvFileToCask("src/test/resources/nicea", niceaType)
      uploadCsvFileToCask("src/test/resources/troy", troyType)
      uploadCsvFileToCask("src/test/resources/smyrna", smyrnaType)
      uploadXmlFiles("src/test/resources/persepolis", persepolisType)
      uploadJsonFiles("src/test/resources/philadelphia", philadelphiaType)
      uploadCsvFileToCask("src/test/resources/magnesia", magnesiaType)
      uploadCsvFileToCask("src/test/resources/knidos", knidosType)
      uploadJsonFiles("src/test/resources/tenedos/trades", tenedosTradesType)
      uploadJsonFiles("src/test/resources/rfq/cb", rfqConvertibleBondsType)
      uploadJsonFiles("src/test/resources/rfq/ird", rfqIrdType)
      uploadCsvFileToCask("src/test/resources/lesbos/OrderSent", lesbosWebOrderSent)
      uploadCsvFileToCask("src/test/resources/lesbos/OrderFilled", lesbosWebOrderFilled)
   }

   fun uploadFile(inputFolder: String, typeName: String, postFunc: (file: File, typeName: String) -> Int) {
      val jsonFiles = File(inputFolder)
      Files.walk(Paths.get(jsonFiles.absolutePath))
         .filter { Files.isRegularFile(it) }
         .forEach { filePath ->
            postFunc(filePath.toFile(), typeName)
         }
      vyneSystem.vyneQueryServer.ensureService("vyne.cask.${typeName}CaskService")
   }

   fun uploadFileOnly(inputFolder: String, typeName: String, postFunc: (file: File, typeName: String) -> Int): MutableList<Int>? {
      val jsonFiles = File(inputFolder)
      return Files.walk(Paths.get(jsonFiles.absolutePath))
         .filter { Files.isRegularFile(it) }
         .map { filePath ->
            postFunc(filePath.toFile(), typeName)
         }.collect(Collectors.toList())

   }

   private fun uploadCsvFileToCask(csvFileFolder: String, typeName: String) {
      uploadFile(csvFileFolder, typeName) {
         file, _ ->  vyneSystem.vyneQueryServer.postCsvDataFile(file, typeName)
      }
   }

   private fun uploadXmlFiles(xmlFileFolder: String, typeName: String) {
      uploadFile(xmlFileFolder, typeName) {
         file, _ ->  vyneSystem.vyneQueryServer.postXmlDataFile(file, typeName)
      }
   }

   private fun uploadJsonFiles(jsonFileFolder: String, typeName: String) {
      uploadFile(jsonFileFolder, typeName) {
         file, _ ->  vyneSystem.vyneQueryServer.postJsonDataFile(file, typeName)
      }
   }

   /**
    * Submits the given Vyne Query against VyneQL Query Point.
    * @param vyneQlQuery VyneQL Query.
    * @return Query Result in csv format.
    */
   fun submitToVyneQlToQueryServer(vyneQueryServerHostPort: String, vyneQlQuery: String): String {
      val request = Request
         .post("http://$vyneQueryServerHostPort/api/vyneql?resultMode=RAW")
         .bodyString(vyneQlQuery, ContentType.APPLICATION_JSON)
         .setHeader("Accept", "text/csv")
         .setHeader("Content-Type", "application/json")
      return request.execute().returnContent().asString()
   }

   fun waitForCaskType(vyneQueryServerHostPort: String, fullyQualifiedName: String) {
      vyneSystem.vyneQueryServer.ensureType(fullyQualifiedName)
   }
}

enum class RunMode {
   Docker,
   Local
}
