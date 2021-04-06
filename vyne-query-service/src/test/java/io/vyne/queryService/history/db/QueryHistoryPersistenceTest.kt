package io.vyne.queryService.history.db

import com.winterbe.expekt.should
import io.vyne.query.ResultMode
import io.vyne.queryService.BaseQueryServiceTest
import io.vyne.queryService.asSimpleQueryResultList
import io.vyne.queryService.history.db.entity.QueryHistoryRecordRepository
import io.vyne.schemas.fqn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import me.eugeniomarletti.kotlin.metadata.shadow.utils.addToStdlib.safeAs
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner

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
   lateinit var historyDbWriter: QueryHistoryDbWriter

   @Before
   fun setup() {
      setupTestService(historyDbWriter)
   }

   @Test
   fun `can read and write query results to db`() = runBlockingTest{
      val query = buildQuery("Order[]")
      val response = queryService.submitQuery(query, ResultMode.SIMPLE, MediaType.APPLICATION_JSON_VALUE)
         .body!!
         .asSimpleQueryResultList()
      val queryHistoryCount = queryHistoryRecordRepository.findAll()
      queryHistoryCount.should.have.size(1)
      response.should.not.be.empty
      response[0].typeName.should.equal("Order[]".fqn())
      response[0].value.safeAs<List<Any>>().should.have.size(1)
      TODO()
   }
}

//@Import(ReactiveDatabaseSupport::class)
@Configuration
@EnableAutoConfiguration
@EnableJpaRepositories(basePackageClasses = [QueryHistoryRecordRepository::class])
@Import(QueryHistoryDbWriter::class)
class TestConfig {

}
