package io.vyne.queryService

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.winterbe.expekt.should
import io.vyne.StubService
import io.vyne.Vyne
import io.vyne.VyneClient
import io.vyne.models.json.parseJsonModel
import io.vyne.query.Query
import io.vyne.query.QueryMode
import io.vyne.query.ResultMode
import io.vyne.query.TypeNameListQueryExpression
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.spring.SimpleVyneProvider
import io.vyne.spring.VyneProvider
import io.vyne.testVyne
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals

@RunWith(SpringRunner::class)
@SpringBootTest(
   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
   properties = [
      "vyne.schema.publicationMethod=LOCAL",
      "spring.main.allow-bean-definition-overriding=true",
      "eureka.client.enabled=false",
      "vyne.query-history.enabled=true",
      "vyne.search.directory=./search/\${random.int}"
   ]
)
class VyneQueryIntegrationTest {

   @Autowired
   private lateinit var restTemplate: TestRestTemplate;


   @LocalServerPort
   val randomServerPort = 0

   object UserSchema {
      val source = """
         namespace io.vyne.queryService {
            type User {
               userId : UserId as String
               userName : Username as String
            }

            service UserService {
               operation getUsers(): User[]
            }
       }


      """.trimIndent()

      val schema = TaxiSchema.from(source, "UserSchema", "0.1.0")

      /**
       * Stub Vyne with a stub service
       */
      fun pipelineTestVyne(): Pair<Vyne, StubService> {
         val src = UserSchema.source.trimIndent()

         return testVyne(src)
      }

   }

   @TestConfiguration
   class SpringConfig {
      @Bean
      @Primary
      fun vyneProvider(): VyneProvider {
         val (vyne, stub) = UserSchema.pipelineTestVyne()
         stub.addResponse(
            "getUsers", vyne.parseJsonModel(
               "io.vyne.queryService.User[]", """
            [{
               "userId": "1010",
               "userName": "jean-pierre"
            },{
               "userId": "2020",
               "userName": "jean-paul"
            },{
               "userId": "3030",
               "userName": "jean-jacques"
            }]
         """.trimIndent()
            )
         )

         return SimpleVyneProvider(vyne)
      }
   }

   @Test
   fun `streaming json response`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE)

      val entity = HttpEntity("findAll { User[] }", headers)

      val response =
         restTemplate.exchange("/api/vyneql/async?resultMode=SIMPLE", HttpMethod.POST, entity, String::class.java)

      response.statusCodeValue.should.be.equal(200)
      response.headers["Content-Type"].should.equal(listOf(MediaType.APPLICATION_JSON_VALUE))

      val responseBody = response.body
      val asyncQueryResponse = jacksonObjectMapper().readValue<Map<String, Any>>(responseBody)
      val queryResult = restTemplate.getForObject(
         "/api/query/history/${asyncQueryResponse["queryId"]}",
         Map::class.java
      ) as Map<String, Any>
      // The query history should've executed the query correctly.
      val users = queryResult.getMap("response")
         .getMap("results")
         .getList("lang.taxi.Array<io.vyne.queryService.User>")
      users.should.have.size(3)
   }

   @Test
   fun `streaming json response with projection`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE)

      val entity = HttpEntity("findAll { User[] } as Username[]", headers)

      val response =
         restTemplate.exchange("/api/vyneql/async?resultMode=SIMPLE", HttpMethod.POST, entity, String::class.java)

      response.statusCodeValue.should.be.equal(200)
      response.headers["Content-Type"].should.equal(listOf(MediaType.APPLICATION_JSON_VALUE))

      val responseBody = response.body!!
      val asyncQueryResponse = jacksonObjectMapper().readValue<Map<String, Any>>(responseBody)

      val queryResult = restTemplate.getForObject(
         "/api/query/history/${asyncQueryResponse["queryId"]}",
         Map::class.java
      ) as Map<String, Any>
      queryResult.getMap("response")
         .getMap("results")
         .getList("lang.taxi.Array<io.vyne.queryService.Username>")
         .should.have.size(3)
   }

   @Test
   fun `Simple JSON POST request should answer json`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE)

      val entity = HttpEntity("findAll { User[] }", headers)

      val response = restTemplate.exchange("/api/vyneql?resultMode=SIMPLE", HttpMethod.POST, entity, String::class.java)

      response.statusCodeValue.should.be.equal(200)
      response.headers["Content-Type"].should.equal(listOf(MediaType.APPLICATION_JSON_VALUE))

      val result = jacksonObjectMapper().readTree(response.body)
      assertEquals(
         result["results"]["lang.taxi.Array<io.vyne.queryService.User>"].toPrettyString(), """
[ {
  "userId" : "1010",
  "userName" : "jean-pierre"
}, {
  "userId" : "2020",
  "userName" : "jean-paul"
}, {
  "userId" : "3030",
  "userName" : "jean-jacques"
} ]""".trimIndent()
      )
   }

   @Test
   fun `DEFAULT RAW  POST request should answer plain json`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.accept = listOf(MediaType.APPLICATION_JSON)
      headers.acceptCharset = listOf(StandardCharsets.UTF_8)

      val entity = HttpEntity("findAll { User[] }", headers)

      val response = restTemplate.exchange("/api/vyneql?resultMode=RAW", HttpMethod.POST, entity, String::class.java)

      response.statusCodeValue.should.be.equal(200)
      response.headers["Content-Type"].should.equal(listOf(MediaType.APPLICATION_JSON_VALUE))
      assertEquals(
         response.body.trimIndent(), """
         [ {
           "userId" : "1010",
           "userName" : "jean-pierre"
         }, {
           "userId" : "2020",
           "userName" : "jean-paul"
         }, {
           "userId" : "3030",
           "userName" : "jean-jacques"
         } ]""".trimIndent()
      )
   }

   @Test
   fun `SIMPLE CSV POST request should answer plain json`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", "text/csv")

      val entity = HttpEntity("findAll { User[] }", headers)

      val response = restTemplate.exchange("/api/vyneql?resultMode=SIMPLE", HttpMethod.POST, entity, String::class.java)

      // SIMPLE should still return JSON even with csv content type
      response.statusCodeValue.should.be.equal(200)
      response.headers["Content-Type"].should.equal(listOf("text/csv;charset=UTF-8"))

      val result = jacksonObjectMapper().readTree(response.body)
      assertEquals(
         result["results"]["lang.taxi.Array<io.vyne.queryService.User>"].toPrettyString(), """
         [ {
           "userId" : "1010",
           "userName" : "jean-pierre"
         }, {
           "userId" : "2020",
           "userName" : "jean-paul"
         }, {
           "userId" : "3030",
           "userName" : "jean-jacques"
         } ]""".trimIndent()
      )
   }

   @Test
   fun `RAW JSON POST request should answer plain json`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE)

      val entity = HttpEntity("findAll { User[] }", headers)

      val response = restTemplate.exchange("/api/vyneql?resultMode=RAW", HttpMethod.POST, entity, String::class.java)

      response.statusCodeValue.should.be.equal(200)
      response.headers["Content-Type"].should.equal(listOf(MediaType.APPLICATION_JSON_VALUE))
      assertEquals(
         response.body.trimIndent(), """
         [ {
           "userId" : "1010",
           "userName" : "jean-pierre"
         }, {
           "userId" : "2020",
           "userName" : "jean-paul"
         }, {
           "userId" : "3030",
           "userName" : "jean-jacques"
         } ]""".trimIndent()
      )
   }

   @Test
   fun `RAW CSV POST request should answer plain csv`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", "text/csv")

      val entity = HttpEntity("findAll { User[] }", headers)

      val response = restTemplate.exchange("/api/vyneql?resultMode=RAW", HttpMethod.POST, entity, String::class.java)

      response.statusCodeValue.should.be.equal(200)
      response.headers["Content-Type"].should.equal(listOf("text/csv;charset=UTF-8"))
      assertEquals(
         response.body.trimIndent(), """
         userId,userName
         1010,jean-pierre
         2020,jean-paul
         3030,jean-jacques
         """.trimIndent()
      )
   }

   @Test
   fun `Vyne Client should query with VyneQL`() {
      val vyneClient = VyneClient("http://localhost:$randomServerPort")

      val response = vyneClient.submitVyneQl("findAll { User[] }")

      val results = response.results

      (results["lang.taxi.Array<io.vyne.queryService.User>"] as List<Any>).should.have.size.equal(3)

   }

   @Test
   fun `Vyne Client should handle Failed Search response`() {
      val vyneClient = VyneClient("http://localhost:$randomServerPort")

      val response = vyneClient.submitVyneQl("findAll { NotAvailableType[] }")
      response.isFullyResolved.should.be.`false`
      response.results.should.have.size(0)
   }

   @Test
   fun `Vyne Client should query with Query`() {
      val vyneClient = VyneClient("http://localhost:$randomServerPort")

      val query = Query(
         TypeNameListQueryExpression(listOf("io.vyne.queryService.User[]")),
         emptyMap(),
         queryMode = QueryMode.GATHER
      )

      val response = vyneClient.submitQuery(query, ResultMode.SIMPLE)

      val results = response.results

      (results["lang.taxi.Array<io.vyne.queryService.User>"] as List<Any>).should.have.size.equal(3)

   }

   @Test
   fun `Vyne Client should query with Query And map to List`() {
      val vyneClient = VyneClient("http://localhost:$randomServerPort")

      val query = Query(
         TypeNameListQueryExpression(listOf("io.vyne.queryService.User[]")),
         emptyMap(),
         queryMode = QueryMode.GATHER
      )

      val response = vyneClient.submitQuery(query, ResultMode.SIMPLE).getResultListFor(User::class)


      response.should.have.size.equal(3)

   }


}


fun Map<String, Any>.getList(key: String): List<Any> {
   try {
      return this.get(key) as List<Any>
   } catch (e: Exception) {
      throw RuntimeException("Cannot access list with key $key", e)
   }
}

fun Map<String, Any>.getMap(key: String): Map<String, Any> {
   try {
      return this.get(key) as Map<String, Any>
   } catch (e: Exception) {
      throw RuntimeException("Cannot access map with key $key", e)
   }
}
