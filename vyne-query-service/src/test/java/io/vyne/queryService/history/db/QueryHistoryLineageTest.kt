package io.vyne.queryService.history.db

import com.jayway.awaitility.Awaitility.await
import com.winterbe.expekt.should
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.vyne.VyneProvider
import io.vyne.history.db.QueryHistoryDbWriter
import io.vyne.history.rest.QueryHistoryService
import io.vyne.models.csv.CsvFormatSpec
import io.vyne.query.ResultMode
import io.vyne.query.ValueWithTypeName
import io.vyne.query.runtime.core.MetricsEventConsumer
import io.vyne.query.runtime.core.QueryResponseFormatter
import io.vyne.query.runtime.core.QueryService
import io.vyne.query.runtime.core.monitor.ActiveQueryMonitor
import io.vyne.queryService.TestSpringConfig
import io.vyne.schema.api.SchemaProvider
import io.vyne.schema.api.SimpleSchemaProvider
import io.vyne.schemas.taxi.TaxiSchema
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalCoroutinesApi
@RunWith(SpringRunner::class)
@Import(TestSpringConfig::class)
@ActiveProfiles("test")
@SpringBootTest(
   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
   properties = [
      "vyne.schema.publicationMethod=LOCAL",
      "spring.main.allow-bean-definition-overriding=true",
      "vyne.search.directory=./search/\${random.int}",
      "vyne.analytics.persistResults=true",
      "vyne.telemetry.enabled=false",
      "spring.datasource.url=jdbc:h2:mem:testdbQueryHistoryLineageTest;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE;MODE=LEGACY"
   ]
)
class QueryHistoryLineageTest {
   @Autowired
   lateinit var historyDbWriter: QueryHistoryDbWriter

   @Autowired
   lateinit var historyService: QueryHistoryService

   @Autowired
   lateinit var vyneProvider: VyneProvider

   lateinit var schemaProvider: SchemaProvider

   @Autowired
   lateinit var testSchema: TaxiSchema

   @Before
   fun setup() {
      schemaProvider = SimpleSchemaProvider(testSchema)
   }

   @Test
   fun `when query has multiple links in lineage then all are returned from history service`() {
      val queryId = UUID.randomUUID().toString()
      val meterRegistry = SimpleMeterRegistry()
      val queryService = QueryService(
         schemaProvider,
         vyneProvider,
         historyDbWriter,
         Jackson2ObjectMapperBuilder().build(),
         ActiveQueryMonitor(),
         MetricsEventConsumer(meterRegistry),
         QueryResponseFormatter(listOf(CsvFormatSpec), schemaProvider)
      )
      runBlocking {
         val results = queryService.submitVyneQlQuery(
            """given { email : EmailAddress = "jimmy@foo.com" } find {AccountBalance }""",
            ResultMode.TYPED,
            MediaType.APPLICATION_JSON_VALUE, clientQueryId = queryId
         ).body.toList()
         val valueWithTypeName = results.first() as ValueWithTypeName
         // Wait for the persistence to finish
         val callable = ConditionCallable {
            historyService.getNodeDetail(valueWithTypeName.queryId!!, valueWithTypeName.valueId, "balance")
         }

         await()
            .atMost(10, TimeUnit.SECONDS)
            .until<Boolean>(callable)

         callable.result!!.block().source.should.not.be.empty
      }
   }
}

class ConditionCallable<T>(val predicate: () -> T?): Callable<Boolean> {
   var result: T? = null
   override fun call(): Boolean {
      return try {
         result = predicate()
         result != null
      } catch (e: Exception) {
         false
      }
   }
}

