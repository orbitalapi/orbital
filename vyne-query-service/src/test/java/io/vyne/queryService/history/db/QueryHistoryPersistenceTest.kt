package io.vyne.queryService.history.db

import app.cash.turbine.test
import com.winterbe.expekt.should
import io.r2dbc.spi.ConnectionFactory
import io.vyne.models.json.Jackson
import io.vyne.query.QueryResponse
import io.vyne.query.ResultMode
import io.vyne.queryService.BaseQueryServiceTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.io.ClassPathResource
import org.springframework.data.r2dbc.connectionfactory.init.CompositeDatabasePopulator
import org.springframework.data.r2dbc.connectionfactory.init.ConnectionFactoryInitializer
import org.springframework.data.r2dbc.connectionfactory.init.ResourceDatabasePopulator
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import java.util.*
import kotlin.time.ExperimentalTime


@ExperimentalTime
@ExperimentalCoroutinesApi
@ContextConfiguration(classes = [TestConfig::class])
@RunWith(SpringRunner::class)
@SpringBootTest(
   classes = [TestConfig::class]
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

   @Before
   fun setup() {
      setupTestService(historyDbWriter)
   }

   @Test
   fun `can read and write query results to db from restful query`() = runBlocking {
      val query = buildQuery("Order[]")
      val response = queryService.submitQuery(query, ResultMode.SIMPLE, MediaType.APPLICATION_JSON_VALUE)
         .body!!
         .test {
            expectItem()
            val queryHistory = queryHistoryRecordRepository.findAll()
               .collectList().block()
            queryHistory.should.have.size(1)
            val historyRecord = queryHistory.first()
            JSONAssert.assertEquals(
               Jackson.defaultObjectMapper.writeValueAsString(query),
               historyRecord.queryJson,
               true
            )

            val results = resultRowRepository.findAllByQueryId(historyRecord.queryId)
               .collectList().block()
            results.should.have.size(1)

            expectComplete()

            queryHistoryRecordRepository.findById(historyRecord.id!!).block()
               .let { updatedHistoryRecord ->
                  // Why sn't this workig?
                  updatedHistoryRecord!!.responseStatus.should.equal(QueryResponse.ResponseStatus.COMPLETED)
                  updatedHistoryRecord.endTime.should.not.be.`null`
               }
         }

   }

   @Test
   fun `can read and write query results from taxiQl query`() = runBlocking {
      val id = UUID.randomUUID().toString()
      queryService.submitVyneQlQuery("findAll { Order[] } as Report[]", clientQueryId = id)
         .body
         .test {
            expectItem()
            val historyRecord = queryHistoryRecordRepository.findByClientQueryId(id)
               .block()
            historyRecord.should.not.be.`null`
            historyRecord.taxiQl.should.equal("findAll { Order[] } as Report[]")
//            historyRecord.responseStatus.should.equal(QueryResponse.ResponseStatus.INCOMPLETE)
//         historyRecord.responseStatus.should.equal(QueryResponse.ResponseStatus.COMPLETED)

            val results = resultRowRepository.findAllByQueryId(historyRecord.queryId)
               .collectList().block()
            results.should.have.size(1)
            expectComplete()

            queryHistoryRecordRepository.findById(historyRecord.id!!).block()
               .let { updatedHistoryRecord ->
                  updatedHistoryRecord!!.responseStatus.should.equal(QueryResponse.ResponseStatus.COMPLETED)
                  updatedHistoryRecord.endTime.should.not.be.`null`
               }
         }


   }
}

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
