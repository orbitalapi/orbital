package io.vyne.queryService

import com.winterbe.expekt.should
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.vyne.history.QueryHistoryConfig
import io.vyne.history.db.HistoryPersistenceJpaConfig
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
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.context.junit4.SpringRunner
import java.util.UUID
import javax.sql.DataSource

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [QueryLineageTestConfig::class])
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
   @Test
   fun `creates sankey lineage chart data`():Unit = runBlocking {
      val f = datasource.connection.metaData.url
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
         service BbgOrderService {
            operation findBbgOrders():BloombergOrder[]
         }
         service ReutersOrderService {
            operation findReutersOrders():ReutersOrder[]
         }
         service BbgTraderService {
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
      val result = queryService.submitVyneQlQuery(
        """ findAll { Order[] } as {
            orderId : OrderId
            firstName : TraderFirstName
            lastName : TraderLastName
            name : String by concat(this.firstName, ' ', this.lastName)
         }[]""",
         clientQueryId = clientQueryId
      ).body.toList()
//      val result = vyne.query("""
//         findAll { Order[] } as {
//            orderId : OrderId
//            name : TraderName
//         }[]
//      """.trimIndent()).typedObjects()
      val summary = queryHistoryRecordRepository.findByClientQueryId(clientQueryId)
      val sankeyReport = sankeyChartRowRepository.findAllByQueryId(summary.queryId)
     sankeyReport.should.have.size(9)
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

@Configuration
@EnableAutoConfiguration
@EnableJpaRepositories
@Import(HistoryPersistenceJpaConfig::class)
class QueryLineageTestConfig
