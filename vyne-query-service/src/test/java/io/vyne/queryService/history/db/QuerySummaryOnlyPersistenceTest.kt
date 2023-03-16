package io.vyne.queryService.history.db

import app.cash.turbine.testIn
import com.jayway.awaitility.Awaitility
import com.winterbe.expekt.should
import io.vyne.history.db.QueryHistoryDbWriter
import io.vyne.history.db.QueryHistoryRecordRepository
import io.vyne.history.db.QueryResultRowRepository
import io.vyne.history.rest.QueryHistoryService
import io.vyne.query.ResultMode
import io.vyne.query.ValueWithTypeName
import io.vyne.queryService.BaseQueryServiceTest
import io.vyne.queryService.TestSpringConfig
import io.vyne.schemas.fqn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner
import java.util.*
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalCoroutinesApi
@RunWith(SpringRunner::class)
//@ActiveProfiles("test")
@Import(TestSpringConfig::class)
@SpringBootTest(
   properties = [
      "vyne.schema.publicationMethod=LOCAL",
      "vyne.search.directory=./search/\${random.int}",
      "vyne.analytics.persistResults=false",
      "vyne.telemetry.enabled=false",
      "vyne.analytics.persistRemoteCallMetadata=false",
      "vyne.analytics.persistRemoteCallResponses=false",
      "spring.datasource.url=jdbc:h2:mem:testdbQuerySummaryOnlyPersistenceTest;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE;MODE=LEGACY"]
)
class QuerySummaryOnlyPersistenceTest : BaseQueryServiceTest() {
   @Autowired
   lateinit var historyDbWriter: QueryHistoryDbWriter

   @Autowired
   lateinit var queryHistoryRecordRepository: QueryHistoryRecordRepository

   @Autowired
   lateinit var resultRowRepository: QueryResultRowRepository

   @Autowired
   lateinit var historyService: QueryHistoryService

   @Test
   fun `Only Query Summary is persisted when vyne history persistResults is false for a taxiQl query`() {
      setupTestService(historyDbWriter)
      val id = UUID.randomUUID().toString()

      runTest {
         val turbine =
            queryService.submitVyneQlQuery("find { Order[] } as Report[]", clientQueryId = id).body.testIn(this)

         val first = turbine.awaitItem()
         first.should.not.be.`null`
         turbine.awaitComplete()
      }

      Awaitility.await().atMost(com.jayway.awaitility.Duration.TEN_SECONDS).until {
         val historyRecord = queryHistoryRecordRepository.findByClientQueryId(id)
         historyRecord!!.endTime != null
      }

      val historyRecord = queryHistoryRecordRepository.findByClientQueryId(id)

      historyRecord.should.not.be.`null`
      historyRecord!!.taxiQl.should.equal("find { Order[] } as Report[]")
      historyRecord.endTime.should.not.be.`null`
      historyRecord.recordCount.should.equal(1)

      val results = resultRowRepository.findAllByQueryId(id)

      results.should.be.empty

      val historyProfileData = historyService.getQueryProfileDataFromClientId(id)
      historyProfileData.block().remoteCalls.should.be.empty
   }

   @Test
   fun `Only Query Summary is persisted when vyne history persistResults is false for a query`() {
      setupTestService(historyDbWriter)
      val id = UUID.randomUUID().toString()

      runBlocking {
         val query = buildQuery("Order[]").copy(queryId = id)

         val turbine =
            queryService.submitQuery(query, ResultMode.TYPED, MediaType.APPLICATION_JSON_VALUE).body.testIn(this)
         val next = turbine.awaitItem() as ValueWithTypeName
         next.typeName.should.equal("Order".fqn().parameterizedName)
         turbine.awaitComplete()
      }

      Awaitility.await().atMost(com.jayway.awaitility.Duration.TEN_SECONDS).until {
         val historyRecord = queryHistoryRecordRepository.findByClientQueryId(id)
         historyRecord!!.endTime != null
      }

      val historyRecord = queryHistoryRecordRepository.findByClientQueryId(id)!!

      historyRecord.should.not.be.`null`
      historyRecord.queryJson.should.not.be.`null`
      historyRecord.endTime.should.not.be.`null`
      historyRecord.recordCount.should.equal(1)

      val results = resultRowRepository.findAllByQueryId(id)

      results.should.be.empty

      val historyProfileData = historyService.getQueryProfileDataFromClientId(id)
      historyProfileData.block().remoteCalls.should.be.empty
   }
}
