package io.vyne.queryService

import com.jayway.awaitility.Awaitility
import com.winterbe.expekt.should
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.vyne.StubService
import io.vyne.Vyne
import io.vyne.history.QueryAnalyticsConfig
import io.vyne.history.db.*
import io.vyne.models.json.parseJson
import io.vyne.models.json.parseKeyValuePair
import io.vyne.query.HistoryEventConsumerProvider
import io.vyne.query.history.QuerySankeyChartRow
import io.vyne.query.history.SankeyNodeType
import io.vyne.testVyne
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import java.util.*
import javax.sql.DataSource

@RunWith(SpringRunner::class)
@Import(TestSpringConfig::class)
@SpringBootTest(
   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
   properties = [
      "vyne.schema.publicationMethod=LOCAL",
      "spring.main.allow-bean-definition-overriding=true",
      "vyne.search.directory=./search/\${random.int}",
      "vyne.analytics.persistRemoteCallMetadata=true",
      "vyne.analytics.persistRemoteCallResponses=false",
      "vyne.telemetry.enabled=false",
      "spring.datasource.url=jdbc:h2:mem:testdbQueryLineageTest;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE;MODE=LEGACY"
   ]
)
@ActiveProfiles("test")
class QueryLineageTest : BaseQueryServiceTest() {
   @Autowired
   lateinit var datasource: DataSource

   @Autowired
   lateinit var queryHistoryRecordRepository: QueryHistoryRecordRepository

   @Autowired
   lateinit var resultRowRepository: QueryResultRowRepository

   @Autowired
   lateinit var lineageRecordRepository: LineageRecordRepository

   @Autowired
   lateinit var remoteCallResponseRepository: RemoteCallResponseRepository

   @Autowired
   lateinit var sankeyChartRowRepository: QuerySankeyChartRowRepository

   @Rule
   @JvmField
   final val tempDir = TemporaryFolder()

   val schema = """
         model Order
         type OrderId inherits String
         type InternalTraderId inherits String
         model Trader {
            id : InternalTraderId
            firstName : TraderFirstName inherits String
            lastName : TraderLastName inherits String
         }
         model BloombergOrder inherits Order {
            orderId : BloombergOrderId inherits OrderId
            traderId : BbgTraderId inherits String
         }
         model ReutersOrder inherits Order {
            orderId : ReutersOrderId inherits OrderId
            traderId : ReutersTraderId inherits String
         }
         service BloombergOrders {
            @HttpOperation(url = "https://fakeurl")
            operation findBbgOrders():BloombergOrder[]
         }
         service ReutersOrders {
            @HttpOperation(url = "https://fakeurl")
            operation findReutersOrders():ReutersOrder[]
         }
         service BloombergTraderService {
            @HttpOperation(url = "https://fakeurl")
            operation resolveBbgTraderId(BbgTraderId):InternalTraderId
         }
         service ReutersTraderService {
            @HttpOperation(url = "https://fakeurl")
            operation resolveReutersTraderId(ReutersTraderId):InternalTraderId
         }
         service TraderService {
            @HttpOperation(url = "https://fakeurl")
            operation lookupTrader(InternalTraderId):Trader
         }
      """


   @FlowPreview
   @Test
   fun `creates sankey lineage chart data`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(schema.trimIndent())
      addStubServiceCalls(stub, vyne)

      val queryService = setupTestService(vyne, stub, buildHistoryConsumer())
      val clientQueryId = UUID.randomUUID().toString()
      queryService.submitVyneQlQuery(
         """ find { Order[] } as {
            orderId : OrderId
            firstName : TraderFirstName
            lastName : TraderLastName
            name : String by concat(this.firstName, ' ', this.lastName)
         }[]""",
         clientQueryId = clientQueryId
      ).body!!.toList()
      Awaitility.await().atMost(com.jayway.awaitility.Duration.TEN_SECONDS).until {
         val historyRecord = queryHistoryRecordRepository.findByClientQueryId(clientQueryId)
         historyRecord!!.endTime != null
      }
      var sankeyReport: List<QuerySankeyChartRow> = emptyList()

      Awaitility.await().atMost(com.jayway.awaitility.Duration.TEN_SECONDS).until<Boolean> {
         sankeyReport =
            sankeyChartRowRepository.findAllByQueryId(queryHistoryRecordRepository.findByClientQueryId(clientQueryId)!!.queryId)
         sankeyReport.size == 15
      }
      sankeyReport.size.should.equal(15)
      // ensure there's a row for each attribute
      sankeyReport
         .filter { it.targetNodeType == SankeyNodeType.AttributeName }
         .map { it.targetNode }
         .distinct()
         .should.have.elements("orderId", "firstName", "lastName", "name")

   }

   private fun addStubServiceCalls(stub: StubService, vyne: Vyne) {
      // stub some data
      stub.addResponse(
         "findReutersOrders", vyne.parseJson(
            "ReutersOrder[]", """[
            |{ "orderId" : "r1", "traderId" : "r-jimmy" },
            |{ "orderId" : "r2", "traderId" : "r-jack" }
            |]
         """.trimMargin()
         ), modifyDataSource = true
      )
      stub.addResponse(
         "findBbgOrders", vyne.parseJson(
            "BloombergOrder[]", """[
            |{ "orderId" : "b1", "traderId" : "b-jimmy" },
            |{ "orderId" : "b2", "traderId" : "b-jack" }
            |]
         """.trimMargin()
         ), modifyDataSource = true
      )
      stub.addResponse(
         "resolveBbgTraderId",
         vyne.parseKeyValuePair("InternalTraderId", "int-jimmy"),
         modifyDataSource = true
      )
      stub.addResponse(
         "resolveReutersTraderId",
         vyne.parseKeyValuePair("InternalTraderId", "int-jimmy"),
         modifyDataSource = true
      )
      stub.addResponse(
         "lookupTrader", vyne.parseJson(
            "Trader", """{
            | "id" : "int-jimmy" , "firstName" : "Jimmy" , "lastName" : "Schmitts"
            |}
         """.trimMargin()
         ), modifyDataSource = true
      )
   }

   @Test
   fun `creates sankey lineage for nested attributes of projection`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(schema.trimIndent())
      addStubServiceCalls(stub, vyne)

      val queryService = setupTestService(vyne, stub, buildHistoryConsumer())
      val clientQueryId = UUID.randomUUID().toString()
      queryService.submitVyneQlQuery(
         """ find { Order[] } as {
            orderId : OrderId
            traderData : {
               firstName : TraderFirstName
               lastName : TraderLastName
               name : String by concat(this.firstName, ' ', this.lastName)
            }
         }[]""",
         clientQueryId = clientQueryId
      ).body!!.toList()
      Awaitility.await().atMost(com.jayway.awaitility.Duration.TEN_SECONDS).until {
         val historyRecord = queryHistoryRecordRepository.findByClientQueryId(clientQueryId)
         historyRecord!!.endTime != null
      }
      var sankeyReport: List<QuerySankeyChartRow> = emptyList()
      val queryId = queryHistoryRecordRepository.findByClientQueryId(clientQueryId)!!.queryId
      Awaitility.await().atMost(com.jayway.awaitility.Duration.TEN_SECONDS).until<Boolean> {
         val allRows = sankeyChartRowRepository.findAll()
         val isEmpty = allRows.size > 0
         sankeyReport = sankeyChartRowRepository.findAllByQueryId(queryId)
         sankeyReport.size == 15
      }
      sankeyReport
         .filter { it.targetNodeType == SankeyNodeType.AttributeName }
         .map { it.targetNode }
         .distinct()
         .should.have.elements("orderId", "traderData/firstName", "traderData/lastName", "traderData/name")
   }

   private fun buildHistoryConsumer(): HistoryEventConsumerProvider {
      return QueryHistoryDbWriter(
         queryHistoryRecordRepository,
         resultRowRepository,
         lineageRecordRepository,
         remoteCallResponseRepository,
         sankeyChartRowRepository,
         config = QueryAnalyticsConfig(
            persistenceQueueStorePath = tempDir.root.toPath()
         ),
         meterRegistry = SimpleMeterRegistry()
      )
   }
}

