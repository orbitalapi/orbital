package com.orbitalhq.queryService

//import com.orbitalhq.testVyne
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.winterbe.expekt.should
import com.orbitalhq.StubService
import com.orbitalhq.Vyne
import com.orbitalhq.VyneProvider
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.formats.csv.CsvFormatSpec
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.models.json.parseJsonModel
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schemaServer.core.repositories.SchemaRepositoryConfigLoader
import com.orbitalhq.schemaServer.core.repositories.lifecycle.RepositorySpecLifecycleEventDispatcher
import com.orbitalhq.schemaStore.LocalValidatingSchemaStoreClient
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.spring.SimpleVyneProvider
import com.orbitalhq.spring.config.TestDiscoveryClientConfig
import com.orbitalhq.testVyne
import org.junit.Test
import org.junit.jupiter.api.BeforeAll
import org.junit.runner.RunWith
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals

@RunWith(SpringRunner::class)
@SpringBootTest(
   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
   properties = [
      "vyne.schema.publisher.method=Local",
      "vyne.schema.consumer.method=Local",
      "spring.main.allow-bean-definition-overriding=true",
      "vyne.search.directory=./search/\${random.int}",
      "vyne.telemetry.enabled=false",
   ]
)
@ActiveProfiles("test")
class VyneQueryIntegrationTest : DatabaseTest() {



   @Autowired
   private lateinit var restTemplate: TestRestTemplate

   @LocalServerPort
   val randomServerPort = 0

   object UserSchema {
      val source = """
         namespace com.orbitalhq.queryService {
            type User {
               userId : UserId as String
               userName : Username as String
            }
            type UserWithNulls {
               userId : UserId as String
               userName : Username as String
            }

            type Empty {
               emptyId : EmptyId as String
               userName : Username as String
            }

            service UserService {
               operation getUsers(): User[]
               operation getUsersWithNulls(): UserWithNulls[]
            }

            service EmptyService {
               operation getEmpties(): Empty[]
            }

            service ModelWithCsvFormatService {
               operation getModelWithCsvFormats(): ModelWithCsvFormat[]
            }

            @com.orbitalhq.formats.Csv(
            delimiter = "|",
            nullValue = "NULL")
            model ModelWithCsvFormat {
               field1: Field1 as String by column("field1")
               field2: Field2 as Int by column("field2")
            }
       }


      """.trimIndent()

      val schema = TaxiSchema.from(source, "UserSchema", "0.1.0")

      /**
       * Stub Vyne with a stub service
       */
      fun pipelineTestVyne(): Pair<Vyne, StubService> {
         val src = source.trimIndent()

         return testVyne(src)
      }

   }

   @TestConfiguration
   // We seem to now have multiple VyneSchemaSourceProviders exposed.
   // Adding
   //@Import(SchemaSourcePrimaryBeanConfig::class)
   @Import(TestDiscoveryClientConfig::class)
   class SpringConfig {

      @MockBean
      lateinit var eventDispatcher: RepositorySpecLifecycleEventDispatcher

      @MockBean
      lateinit var configLoader : SchemaRepositoryConfigLoader

      @Bean
      @Primary
      fun schemaProvider(): SchemaProvider = TestSchemaProvider.withBuiltInsAnd(UserSchema.schema)

      @Bean
      fun schemaStore(): SchemaStore = LocalValidatingSchemaStoreClient()

      @Bean
      @Primary
      fun vyneProvider(): VyneProvider {
         val (vyne, stub) = UserSchema.pipelineTestVyne()
         stub.addResponse(
            "getUsers", vyne.parseJsonModel(
               "com.orbitalhq.queryService.User[]", """
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
         stub.addResponse(
            "getUsersWithNulls", vyne.parseJson(
               "com.orbitalhq.queryService.UserWithNulls[]", """
            [{
               "userId": "1010",
               "userName": null
            },{
               "userId": "2020",
               "userName": null
            },{
               "userId": "3030",
               "userName": null
            }]
         """.trimIndent()
            )
         )

         stub.addResponse(
            "getEmpties", vyne.parseJsonModel(
               "com.orbitalhq.queryService.Empty[]", """
            []
         """.trimIndent()
            )
         )

         val csv = """field1|field2
str1|1
str2|2"""

         val stubModelWithCsvFormats = TypedInstance.from(
            UserSchema.schema.type("com.orbitalhq.queryService.ModelWithCsvFormat[]"),
            csv,
            UserSchema.schema,
            formatSpecs = listOf(CsvFormatSpec)
         ) as TypedCollection

         stub.addResponse("getModelWithCsvFormats", stubModelWithCsvFormats)
         return SimpleVyneProvider(vyne)
      }
   }

   @Test
   fun `Simple JSON POST request should answer json`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE)

      val entity = HttpEntity("find { User[] }", headers)

      val response = restTemplate.exchange("/api/vyneql?resultMode=RAW", HttpMethod.POST, entity, String::class.java)

      response.statusCodeValue.should.be.equal(200)
      response.headers["Content-Type"].should.equal(listOf(MediaType.APPLICATION_JSON_VALUE))

      val result = jacksonObjectMapper().readTree(response.body)

      assertEquals(
         result.toPrettyString(), """
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
   fun `Simple TEXT_EVENT_STREAM_VALUE POST request should answer stream`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", MediaType.TEXT_EVENT_STREAM_VALUE)

      val entity = HttpEntity("find { User[] }", headers)

      val response = restTemplate.exchange("/api/vyneql?resultMode=RAW", HttpMethod.POST, entity, String::class.java)

      response.statusCodeValue.should.be.equal(200)
      response.headers["Content-Type"].should.equal(listOf("text/event-stream;charset=UTF-8"))
      response.body!!.withoutWhitespace().should.equal(
         """
            data:{"userId":"1010","userName":"jean-pierre"}
            data:{"userId":"2020","userName":"jean-paul"}
            data:{"userId":"3030","userName":"jean-jacques"}
         """.trimIndent().withoutWhitespace()
      )
   }

   @Test
   fun `When No Path Found Response should be Http 200 for streaming request`() {
      //Username
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", MediaType.TEXT_EVENT_STREAM_VALUE)

      val entity = HttpEntity("find { com.orbitalhq.queryService.Username[] }", headers)

      val response = restTemplate.exchange("/api/vyneql?resultMode=RAW", HttpMethod.POST, entity, String::class.java)

      response.statusCodeValue.should.be.equal(200)
      response.body.should.contain("No data sources were found that can return Username[]")
   }

   @Test
   fun `When No Path Found Response should be Http 400 for non-streaming request`() {
      //Username
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE)

      val entity = HttpEntity("find { com.orbitalhq.Username[] }", headers)

      val response = restTemplate.exchange("/api/vyneql?resultMode=RAW", HttpMethod.POST, entity, String::class.java)

      response.statusCodeValue.should.be.equal(400)
   }

   @Test
   fun `DEFAULT RAW  POST request should answer plain json`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.accept = listOf(MediaType.APPLICATION_JSON)
      headers.acceptCharset = listOf(StandardCharsets.UTF_8)

      val entity = HttpEntity("find { User[] }", headers)

      val response = restTemplate.exchange("/api/vyneql?resultMode=RAW", HttpMethod.POST, entity, String::class.java)

      response.statusCodeValue.should.be.equal(200)
      response.headers["Content-Type"].should.equal(listOf(MediaType.APPLICATION_JSON_VALUE))

      val result = jacksonObjectMapper().readTree(response.body)

      assertEquals(
         result.toPrettyString(), """
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

      val entity = HttpEntity("find { User[] }", headers)

      val response = restTemplate.exchange("/api/vyneql?resultMode=RAW", HttpMethod.POST, entity, String::class.java)

      response.statusCodeValue.should.be.equal(200)
      response.headers["Content-Type"].should.equal(listOf(MediaType.APPLICATION_JSON_VALUE))
      val result = jacksonObjectMapper().readTree(response.body)
      assertEquals(
         result.toPrettyString(), """
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
   fun `Formatted POST request on anonymous type should match expected result`() {
      val headers = HttpHeaders()
      headers.set("Accept", "text/csv")
      val entity = HttpEntity(
         """find { User[] } as
        @com.orbitalhq.formats.Csv(
            delimiter = "|",
            nullValue = "NULL",
            useFieldNamesAsColumnNames = true
         )
         {
            id : UserId
            name : com.orbitalhq.queryService.Username
         }[]
         """.trimMargin(), headers
      )

      val response = restTemplate.exchange("/api/vyneql", HttpMethod.POST, entity, String::class.java)

      response.statusCodeValue.should.be.equal(200)
      val responseText = response.body!!
         .replace("\r\n", "\n")
         .trim()
      val expected = """id|name
1010|jean-pierre
2020|jean-paul
3030|jean-jacques"""
      responseText.should.equal(expected)
   }

   @Test
   fun `RAW CSV POST request should answer plain csv`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", "text/csv")

      val entity = HttpEntity("find { User[] }", headers)

      val response = restTemplate.exchange("/api/vyneql?resultMode=RAW", HttpMethod.POST, entity, String::class.java)

      response.statusCodeValue.should.be.equal(200)
      response.headers["Content-Type"].should.equal(listOf("text/csv;charset=UTF-8"))
      assertEquals(
         response.body!!.trimIndent(), """
         userId,userName
         1010,jean-pierre
         2020,jean-paul
         3030,jean-jacques
         """.trimIndent()
      )
   }

   @Test
   fun `Request for no strategy results should return empty list`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE)

      val entity = HttpEntity("find { Empty[] }", headers)

      val response = restTemplate.exchange("/api/vyneql?resultMode=RAW", HttpMethod.POST, entity, String::class.java)

      response.statusCodeValue.should.be.equal(200)
      response.headers["Content-Type"].should.equal(listOf(MediaType.APPLICATION_JSON_VALUE))

      val result = jacksonObjectMapper().readTree(response.body)

      assertEquals(
         result.toPrettyString(), """
      [ ]""".trimIndent()
      )

   }

   @Test
   fun `A RAW Request for a type with model spec should return model spec formatted response`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", "text/csv")
      val entity = HttpEntity("find { ModelWithCsvFormat[] }", headers)
      val response = restTemplate.exchange("/api/vyneql?resultMode=RAW", HttpMethod.POST, entity, String::class.java)
      response.statusCodeValue.should.be.equal(200)
      response.headers["Content-Type"].should.equal(listOf("text/csv;charset=UTF-8"))
      assertEquals(
         """
         field1|field2
         str1|1
         str2|2
         """.trimIndent(),
         response.body!!.trimIndent()
      )
   }

   @Test
   fun `a RAW request with nulls omitted doesnt include nulls in response`() {
      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE)
      val entity = HttpEntity("@OmitNulls query MyQuery { find { UserWithNulls[] } }", headers)
      val response = restTemplate.exchange("/api/vyneql?resultMode=RAW", HttpMethod.POST, entity, String::class.java)
      response.statusCodeValue.should.be.equal(200)
      response.headers["Content-Type"].should.equal(listOf("application/json"))
      // The null keys shouldn't be present
      val expected = """[{"userId":"1010"},{"userId":"2020"},{"userId":"3030"}]"""
      JSONAssert.assertEquals(expected, response.body, true)
   }


}

