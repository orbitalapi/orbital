package io.vyne.queryService.history.db

import app.cash.turbine.test
import com.jayway.awaitility.Awaitility.await
import com.jayway.awaitility.Duration
import com.winterbe.expekt.should
import io.vyne.query.ResultMode
import io.vyne.queryService.BaseQueryServiceTest
import io.vyne.queryService.history.QueryHistoryService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.seconds


@ExperimentalTime
@ExperimentalCoroutinesApi
//@ContextConfiguration(classes = [TestConfig::class])
@RunWith(SpringRunner::class)
@SpringBootTest(
   //classes = [TestConfig::class]
)
class QueryHistoryPersistenceTest : BaseQueryServiceTest() {

   @Autowired
   lateinit var queryHistoryRecordRepository: QueryHistoryRecordRepository

   @Autowired
   lateinit var resultRowRepository: QueryResultRowRepository

   @Autowired
   lateinit var lineageRepository: LineageRecordRepository

   @Autowired
   lateinit var historyDbWriter: QueryHistoryDbWriter

   @Autowired
   lateinit var historyService: QueryHistoryService

   @Before
   fun setup() {
      setupTestService(historyDbWriter)
   }

   @Test
   @Ignore
   fun `can read and write query results to db from restful query`() {
      val query = buildQuery("Order[]")
      val id = query.queryId

      runBlocking {
         val response = queryService.submitQuery(query, ResultMode.SIMPLE, MediaType.APPLICATION_JSON_VALUE)
            .body!!
            .test {
               val first = expectItem()
               expectComplete()
            }
      }

      Thread.sleep(2000)
      val results = resultRowRepository.findAllByQueryId(id)
         .collectList().block()
      results.should.have.size(1)

      val queryHistory = queryHistoryRecordRepository.findByQueryId(id).block()

      queryHistoryRecordRepository.findById(queryHistory.id!!).block()
         .let { updatedHistoryRecord ->
            // Why sn't this workig?
            updatedHistoryRecord.endTime.should.not.be.`null`
         }
   }

   @Test
   fun `can read and write query results from taxiQl query`()  {
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

      await().atMost(Duration.TEN_SECONDS).until {
         val historyRecord = queryHistoryRecordRepository.findByClientQueryId(id)
            .block()
         historyRecord != null && historyRecord.endTime != null
      }

      val historyRecord = queryHistoryRecordRepository.findByClientQueryId(id)
         .block()
      historyRecord.should.not.be.`null`
      historyRecord.taxiQl.should.equal("findAll { Order[] } as Report[]")
      historyRecord.endTime.should.not.be.`null`

      val results = resultRowRepository.findAllByQueryId(id)
         .collectList().block()
      results.should.have.size(1)

      val historyProfileData = historyService.getQueryProfileDataFromClientId(id).block()!!
      historyProfileData.remoteCalls.should.have.size(3)
   }
}


/*
@Configuration
@EnableAutoConfiguration
@EnableR2dbcRepositories(basePackageClasses = [QueryHistoryRecordRepository::class])
@Import(QueryHistoryDbWriter::class)
class TestConfig {
   @Bean
   fun initializer(@Qualifier("connectionFactory") connectionFactory: ConnectionFactory): ConnectionFactoryInitializer? {
      val initializer = ConnectionFactoryInitializer()
      initializer.setConnectionFactory(connectionFactory)
      val populator = CompositeDatabasePopulator()
      populator.addPopulators(ResourceDatabasePopulator(ClassPathResource("schema.sql")))
      initializer.setDatabasePopulator(populator)
      return initializer
   }
}

*/
