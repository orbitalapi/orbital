package io.vyne.queryService.history.db

import app.cash.turbine.test
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
import io.vyne.queryService.query.FirstEntryMetadataResultSerializer
import io.vyne.schemas.fqn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import java.util.UUID
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@ExperimentalTime
@ExperimentalCoroutinesApi
@RunWith(SpringRunner::class)
//@ActiveProfiles("test")
@Import(TestSpringConfig::class)
@SpringBootTest(properties = [
   "vyne.schema.publicationMethod=LOCAL",
   "vyne.search.directory=./search/\${random.int}",
   "vyne.analytics.persistResults=false",
   "spring.datasource.url=jdbc:h2:mem:testdbQuerySummaryOnlyPersistenceTest;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE;MODE=LEGACY"])
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

      runBlocking {
         queryService.submitVyneQlQuery("findAll { Order[] } as Report[]", clientQueryId = id)
            .body
            .test(timeout = 10.seconds) {
               val first = expectItem()
               first.should.not.be.`null`
               expectComplete()
            }
      }

      Awaitility.await().atMost(com.jayway.awaitility.Duration.TEN_SECONDS).until {
         val historyRecord = queryHistoryRecordRepository.findByClientQueryId(id)
         historyRecord!!.endTime != null
      }

      val historyRecord = queryHistoryRecordRepository.findByClientQueryId(id)

      historyRecord.should.not.be.`null`
      historyRecord!!.taxiQl.should.equal("findAll { Order[] } as Report[]")
      historyRecord!!.endTime.should.not.be.`null`
      historyRecord!!.recordCount.should.equal(1)

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
         queryService.submitQuery(query, ResultMode.SIMPLE, MediaType.APPLICATION_JSON_VALUE)
            .body
            .test(timeout = 10.seconds) {
               val next = expectItem() as ValueWithTypeName
               next.typeName.should.equal("Order".fqn().parameterizedName)
               expectComplete()
            }
      }

      Awaitility.await().atMost(com.jayway.awaitility.Duration.TEN_SECONDS).until {
         val historyRecord = queryHistoryRecordRepository.findByClientQueryId(id)
         historyRecord!!.endTime != null
      }

      val historyRecord = queryHistoryRecordRepository.findByClientQueryId(id)

      historyRecord.should.not.be.`null`
      historyRecord!!.queryJson.should.not.be.`null`
      historyRecord!!.endTime.should.not.be.`null`
      historyRecord!!.recordCount.should.equal(1)

      val results = resultRowRepository.findAllByQueryId(id)

      results.should.be.empty

      val historyProfileData = historyService.getQueryProfileDataFromClientId(id)
      historyProfileData.block().remoteCalls.should.be.empty
   }
}
