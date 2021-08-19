package io.vyne.queryService

import com.jayway.awaitility.Awaitility
import com.winterbe.expekt.should
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.vyne.history.QueryHistoryConfig
import io.vyne.history.db.LineageRecordRepository
import io.vyne.history.db.QueryHistoryDbWriter
import io.vyne.history.db.QueryHistoryRecordRepository
import io.vyne.history.db.QueryResultRowRepository
import io.vyne.history.db.QuerySankeyChartRowRepository
import io.vyne.history.db.RemoteCallResponseRepository
import io.vyne.models.json.parseJson
import io.vyne.models.json.parseKeyValuePair
import io.vyne.query.HistoryEventConsumerProvider
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
import org.springframework.test.context.junit4.SpringRunner
import java.util.UUID
import javax.sql.DataSource

@RunWith(SpringRunner::class)
@Import(TestSpringConfig::class)
@SpringBootTest(
   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
   properties = [
      "vyne.schema.publicationMethod=LOCAL",
      "spring.main.allow-bean-definition-overriding=true",
      "eureka.client.enabled=false",
      "vyne.search.directory=./search/\${random.int}"
   ])
class QueryLineageTest : BaseQueryServiceTest() {
   @Autowired
   lateinit var datasource:DataSource

   @Autowired
   lateinit var  queryHistoryRecordRepository: QueryHistoryRecordRepository
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
   @FlowPreview
   @Test
   fun `creates sankey lineage chart data`():Unit = runBlocking {
      datasource.connection.metaData.url
      val (vyne,stub) = testVyne("""
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
            operation findBbgOrders():BloombergOrder[]
         }
         service ReutersOrders {
            operation findReutersOrders():ReutersOrder[]
         }
         service BloombergTraderService {
            operation resolveBbgTraderId(BbgTraderId):InternalTraderId
         }
         service ReutersTraderService {
            operation resolveReutersTraderId(ReutersTraderId):InternalTraderId
         }
         service TraderService {
            operation lookupTrader(InternalTraderId):Trader
         }
      """.trimIndent())
      // stub some data
      stub.addResponse("findReutersOrders", vyne.parseJson("ReutersOrder[]", """[
         |{ "orderId" : "r1", "traderId" : "r-jimmy" },
         |{ "orderId" : "r2", "traderId" : "r-jack" }
         |]
      """.trimMargin()), modifyDataSource = true)
      stub.addResponse("findBbgOrders", vyne.parseJson("BloombergOrder[]","""[
         |{ "orderId" : "b1", "traderId" : "b-jimmy" },
         |{ "orderId" : "b2", "traderId" : "b-jack" }
         |]
      """.trimMargin()), modifyDataSource = true)
      stub.addResponse("resolveBbgTraderId", vyne.parseKeyValuePair("InternalTraderId", "int-jimmy"), modifyDataSource = true)
      stub.addResponse("resolveReutersTraderId", vyne.parseKeyValuePair("InternalTraderId", "int-jimmy"), modifyDataSource = true)
      stub.addResponse("lookupTrader", vyne.parseJson("Trader", """{
         | "id" : "int-jimmy" , "firstName" : "Jimmy" , "lastName" : "Schmitts"
         |}
      """.trimMargin()), modifyDataSource = true)

      val queryService = setupTestService(vyne,stub,buildHistoryConsumer())
      val clientQueryId = UUID.randomUUID().toString()
      queryService.submitVyneQlQuery(
        """ findAll { Order[] } as {
            orderId : OrderId
            firstName : TraderFirstName
            lastName : TraderLastName
            name : String by concat(this.firstName, ' ', this.lastName)
         }[]""",
         clientQueryId = clientQueryId
      ).body!!.toList()
      Awaitility.await().atMost(com.jayway.awaitility.Duration.TEN_SECONDS).until {
         val historyRecord = queryHistoryRecordRepository.findByClientQueryId(clientQueryId)
         historyRecord.endTime != null
      }
      Awaitility.await().atMost(com.jayway.awaitility.Duration.TEN_SECONDS).until {
         val sankeyReport = sankeyChartRowRepository.findAllByQueryId(queryHistoryRecordRepository.findByClientQueryId(clientQueryId).queryId)
         sankeyReport.size == 15
      }
   }

   private fun buildHistoryConsumer(): HistoryEventConsumerProvider {
      return QueryHistoryDbWriter(
         queryHistoryRecordRepository,
         resultRowRepository,
         lineageRecordRepository,
         remoteCallResponseRepository,
         sankeyChartRowRepository,
         config = QueryHistoryConfig(
            persistenceQueueStorePath = tempDir.root.toPath()
         ),
         meterRegistry = SimpleMeterRegistry()
      )
   }
}

