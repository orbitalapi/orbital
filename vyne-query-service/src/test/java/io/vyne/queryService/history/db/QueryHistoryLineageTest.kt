package io.vyne.queryService.history.db

import com.jayway.awaitility.Awaitility.await
import com.winterbe.expekt.should
import io.vyne.models.TypedInstance
import io.vyne.models.json.parseJson
import io.vyne.models.json.parseKeyValuePair
import io.vyne.query.ResultMode
import io.vyne.queryService.BaseQueryServiceTest
import io.vyne.queryService.history.QueryHistoryService
import io.vyne.queryService.history.QueryResultNodeDetail
import io.vyne.queryService.query.FirstEntryMetadataResultSerializer
import io.vyne.testVyne
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalCoroutinesApi
//@ContextConfiguration(classes = [TestConfig::class])
@RunWith(SpringRunner::class)
@SpringBootTest(
   //classes = [TestConfig::class]
)
class QueryHistoryLineageTest : BaseQueryServiceTest() {
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

   @Test
   fun `when query has multiple links in lineage then all are returned from history service`() {
      val (vyne, stub) = testVyne(
         """
         type EmailAddress inherits String
         type PersonId inherits Int
         type LoyaltyCardNumber inherits String
         type BalanceInPoints inherits Decimal
         model AccountBalance {
            balance: BalanceInPoints
         }
         service Service {
            operation findPersonIdByEmail(EmailAddress):PersonId
            operation findMembership(PersonId):LoyaltyCardNumber
            operation findBalance(LoyaltyCardNumber):AccountBalance
         }
      """
      )
      // setup stubs
      stub.addResponse("findPersonIdByEmail", TypedInstance.from(vyne.type("PersonId"), 1, vyne.schema), modifyDataSource = true)
      stub.addResponse(
         "findMembership",
         vyne.parseKeyValuePair("LoyaltyCardNumber", "1234-5678"),
         modifyDataSource = true
      )
      stub.addResponse("findBalance", vyne.parseJson("AccountBalance", """{ "balance" : 100 }"""), modifyDataSource = true)

      val queryId = UUID.randomUUID().toString()
      val queryService = setupTestService(vyne, stub, historyDbWriter)
      runBlocking {
         val results = queryService.submitVyneQlQuery(
            """given { email : EmailAddress = "jimmy@foo.com" } findOne { AccountBalance }""",
            ResultMode.SIMPLE,
            MediaType.APPLICATION_JSON_VALUE, clientQueryId = queryId
         ).body.toList()
         val valueWithTypeName = results.first() as FirstEntryMetadataResultSerializer.ValueWithTypeName
         // Wait for the persistence to finish
         var lineage: QueryResultNodeDetail? = null
         await().atMost(10, TimeUnit.SECONDS)
            .until {
               try {
                  lineage = historyService.getNodeDetail(valueWithTypeName.queryId!!, valueWithTypeName.valueId, "balance")
                     .block()!!
                  lineage != null
               } catch (e: Exception) {
                  false
               }
            }

         lineage!!.source.should.not.be.empty
      }
   }
}
