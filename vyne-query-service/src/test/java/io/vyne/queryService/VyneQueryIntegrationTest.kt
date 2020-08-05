package io.vyne.queryService

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.winterbe.expekt.should
import io.vyne.StubService
import io.vyne.Vyne
import io.vyne.VyneClient
import io.vyne.models.json.parseJsonModel
import io.vyne.query.*
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.spring.SimpleVyneProvider
import io.vyne.spring.VyneProvider
import io.vyne.testVyne
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.*
import org.springframework.test.context.junit4.SpringRunner
import kotlin.test.assertEquals

@RunWith(SpringRunner::class)
@SpringBootTest(
   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
   properties = [
      "vyne.schema.publicationMethod=LOCAL",
      "spring.main.allow-bean-definition-overriding=true",
      "eureka.client.enabled=false",
      "vyne.search.directory=./search/\${random.int}"
   ])
class VyneQueryIntegrationTest {

   @Autowired
   private lateinit var restTemplate: TestRestTemplate;



   @LocalServerPort
   val randomServerPort = 0

   object UserSchema {
      val source = """

         type User {
            userId : UserId as String
            userName : Username as String
         }

         service UserService {
            operation getUsers(): User[]
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
         stub.addResponse("getUsers", vyne.parseJsonModel("User[]", """
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
         """.trimIndent()))

         return SimpleVyneProvider(vyne)
      }
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
      assertEquals(result["results"]["lang.taxi.Array<User>"].toPrettyString(), """
[ {
  "userId" : "1010",
  "userName" : "jean-pierre"
}, {
  "userId" : "2020",
  "userName" : "jean-paul"
}, {
  "userId" : "3030",
  "userName" : "jean-jacques"
} ]""".trimIndent())
   }

   @Test
   fun `DEFAULT RAW  POST request should answer plain json`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      // NO Accept Header

      val entity = HttpEntity("findAll { User[] }", headers)

      val response = restTemplate.exchange("/api/vyneql?resultMode=RAW", HttpMethod.POST, entity, String::class.java)

      response.statusCodeValue.should.be.equal(200)
      response.headers["Content-Type"].should.equal(listOf(MediaType.APPLICATION_JSON_VALUE))
      assertEquals(response.body.trimIndent(), """
         [ {
           "userId" : "1010",
           "userName" : "jean-pierre"
         }, {
           "userId" : "2020",
           "userName" : "jean-paul"
         }, {
           "userId" : "3030",
           "userName" : "jean-jacques"
         } ]""".trimIndent())
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
      assertEquals(result["results"]["lang.taxi.Array<User>"].toPrettyString(), """
         [ {
           "userId" : "1010",
           "userName" : "jean-pierre"
         }, {
           "userId" : "2020",
           "userName" : "jean-paul"
         }, {
           "userId" : "3030",
           "userName" : "jean-jacques"
         } ]""".trimIndent())
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
      assertEquals(response.body.trimIndent(), """
         [ {
           "userId" : "1010",
           "userName" : "jean-pierre"
         }, {
           "userId" : "2020",
           "userName" : "jean-paul"
         }, {
           "userId" : "3030",
           "userName" : "jean-jacques"
         } ]""".trimIndent())
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
      assertEquals(response.body.trimIndent(), """
         userId,userName
         1010,jean-pierre
         2020,jean-paul
         3030,jean-jacques
         """.trimIndent())
   }

   @Test
   fun `Vyne Client should query with VyneQL`() {
      val vyneClient = VyneClient("http://localhost:$randomServerPort")

      val response = vyneClient.submitVyneQl("findAll { User[] }")

      val results = response.results

      (results["lang.taxi.Array<User>"] as List<Any>).should.have.size.equal(3)

   }

   @Test
   fun `Vyne Client should query with Query`() {
      val vyneClient = VyneClient("http://localhost:$randomServerPort")

      val query = Query(
         TypeNameListQueryExpression(listOf("User[]")),
         emptyMap(),
         queryMode = QueryMode.GATHER,
         resultMode = ResultMode.SIMPLE)

      val response = vyneClient.submitQuery(query)

      val results = response.results

      (results["lang.taxi.Array<User>"] as List<Any>).should.have.size.equal(3)

   }
}
