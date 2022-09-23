package io.vyne.cask.query.vyneql

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.winterbe.expekt.should
import io.vyne.cask.config.CaskQueryDispatcherConfiguration
import io.vyne.cask.query.BaseCaskIntegrationTest
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import lang.taxi.types.TaxiQLQueryString
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import reactor.test.StepVerifier
import java.time.Duration
import java.util.stream.Collectors
import kotlin.streams.toList

class VyneQlQueryServiceTest : BaseCaskIntegrationTest() {
   val schema = """
      type FirstName inherits String
      type Age inherits Int
      type LastLoggedIn inherits LoginTime
      type LoginTime inherits Instant
      model Person {
         firstName : FirstName
         age : Age
         lastLogin : LastLoggedIn
      }
   """

   lateinit var service: VyneQlQueryService
   private val mapper = jacksonObjectMapper().registerModule(Jdk8Module())
   private val typeRef: TypeReference<List<Map<String, Any>>> = object : TypeReference<List<Map<String, Any>>>() {}

   @Before
   override fun setup() {
      super.setup()
      schemaProvider.updateSource(schema)
      val schema = schemaProvider.schema as TaxiSchema
      val person = schema.versionedType("Person".fqn())
      service = VyneQlQueryService(
         jdbcStreamingTemplate, VyneQlSqlGenerator(
            schemaProvider, configRepository
         ), CaskQueryDispatcherConfiguration()
      )
      val json =
         """[
         { "firstName" : "Jimmy", "age" : 35, "lastLogin" : "2020-11-16T11:47:00Z" },
         { "firstName" : "Jack", "age" : 32, "lastLogin" : "2020-11-15T11:47:00Z" },
         { "firstName" : "John", "age" : 55, "lastLogin" : "2020-10-15T11:47:00Z" }]"""

      ingestJsonData(json, person, schema)
   }


   @Test
   fun findMatchingNoCriteriaReturnsEmptyList() {
      val stream =
         runBlocking { service.submitVyneQlQuery("""find { Person[]( FirstName == "Nobody" ) }""").body.toList() }
      // Jackson stream serialiser invokes 'close' on the stream which in turn inkoves the close handler attached in JdbcStreamingTemplate!
      val jsonString = mapper.writeValueAsString(stream)
      val response = mapper.readValue(jsonString, typeRef)
      response.should.be.empty
      connectionCountingDataSource.connectionList.all { it.isClosed }.should.be.`true`
   }

   //
   @Test
   fun canExecuteFindAll() {
      val stream = runBlocking { service.submitVyneQlQuery("""find { Person[] }""").body.toList() }
      // Jackson stream serialiser invokes 'close' on the stream which in turn inkoves the close handler attached in JdbcStreamingTemplate!
      val jsonString = mapper.writeValueAsString(stream)
      val response = mapper.readValue(jsonString, typeRef)
      response.should.have.size(3)
      response.map { it["firstName"] }.should.have.elements("Jimmy", "Jack", "John")
      connectionCountingDataSource.connectionList.all { it.isClosed }.should.be.`true`
   }

   @Test
   fun `can find with greater than number`() {
      val stream = runBlocking { service.submitVyneQlQuery("""find { Person[]( Age > 33 ) }""").body.toList() }
      // Jackson stream serialiser invokes 'close' on the stream which in turn inkoves the close handler attached in JdbcStreamingTemplate!
      val jsonString = mapper.writeValueAsString(stream)
      val response = mapper.readValue(jsonString, typeRef)
      response.should.have.size(2)
      response.map { it["firstName"] }.should.have.elements("Jimmy", "John")
      connectionCountingDataSource.connectionList.all { it.isClosed }.should.be.`true`
   }

   @Test
   fun `can find with greater then or equal number`() {
      val stream = runBlocking { service.submitVyneQlQuery("""find { Person[]( Age >= 35 ) }""").body.toList() }
      // Jackson stream serialiser invokes 'close' on the stream which in turn inkoves the close handler attached in JdbcStreamingTemplate!
      val jsonString = mapper.writeValueAsString(stream)
      val response = mapper.readValue(jsonString, typeRef)
      response.should.have.size(2)
      response.map { it["firstName"] }.should.have.elements("Jimmy", "John")
      connectionCountingDataSource.connectionList.all { it.isClosed }.should.be.`true`
   }

   @Test
   fun `can find with less than number`() {
      val stream = runBlocking { service.submitVyneQlQuery("""find { Person[]( Age < 33 ) }""").body.toList() }
      // Jackson stream serialiser invokes 'close' on the stream which in turn inkoves the close handler attached in JdbcStreamingTemplate!
      val jsonString = mapper.writeValueAsString(stream)
      val response = mapper.readValue(jsonString, typeRef)
      response.should.have.size(1)
      response.map { it["firstName"] }.should.have.elements("Jack")
      connectionCountingDataSource.connectionList.all { it.isClosed }.should.be.`true`
   }

   @Test
   fun `can find with less or equal than number`() {
      val stream = runBlocking { service.submitVyneQlQuery("""find { Person[]( Age <= 35 ) }""").body.toList() }
      // Jackson stream serialiser invokes 'close' on the stream which in turn inkoves the close handler attached in JdbcStreamingTemplate!
      val jsonString = mapper.writeValueAsString(stream)
      val response = mapper.readValue(jsonString, typeRef)
      response.should.have.size(2)
      response.map { it["firstName"] }.should.have.elements("Jack", "Jimmy")
      connectionCountingDataSource.connectionList.all { it.isClosed }.should.be.`true`
   }

   @Test
   fun `can find with date after`() {
      val stream =
         runBlocking { service.submitVyneQlQuery("""find { Person[]( LastLoggedIn > '2020-11-01T00:00:00Z' ) }""").body.toList() }
      // Jackson stream serialiser invokes 'close' on the stream which in turn inkoves the close handler attached in JdbcStreamingTemplate!
      val jsonString = mapper.writeValueAsString(stream)
      val response = mapper.readValue(jsonString, typeRef)
      response.should.have.size(2)
      response.map { it["firstName"] }.should.have.elements("Jack", "Jimmy")
      connectionCountingDataSource.connectionList.all { it.isClosed }.should.be.`true`
   }

   @Test
   fun `can find with date between`() {
      val stream =
         runBlocking { service.submitVyneQlQuery("""find { Person[]( LastLoggedIn > "2020-11-15T00:00:00Z", LastLoggedIn <= "2020-11-15T23:59:59Z" ) }""").body.toList() }
      // Jackson stream serialiser invokes 'close' on the stream which in turn inkoves the close handler attached in JdbcStreamingTemplate!
      val jsonString = mapper.writeValueAsString(stream)
      val response = mapper.readValue(jsonString, typeRef)
      response.should.have.size(1)
      response.map { it["firstName"] }.should.have.elements("Jack")
      connectionCountingDataSource.connectionList.all { it.isClosed }.should.be.`true`
   }

   @Test
   fun `can query with an abstract property type`() {
      val stream =
         runBlocking { service.submitVyneQlQuery("""find { Person[]( LoginTime > "2020-11-15T00:00:00", LoginTime <= "2020-11-15T23:59:59" ) }""").body.toList() }
      // Jackson stream serialiser invokes 'close' on the stream which in turn inkoves the close handler attached in JdbcStreamingTemplate!
      val jsonString = mapper.writeValueAsString(stream)
      val response = mapper.readValue(jsonString, typeRef)
      response.should.have.size(1)
      response.map { it["firstName"] }.should.have.elements("Jack")
      connectionCountingDataSource.connectionList.all { it.isClosed }.should.be.`true`
   }

   @Test
   fun `can query date without timezone information provided`() {
      val stream =
         runBlocking { service.submitVyneQlQuery("""find { Person[]( LastLoggedIn > "2020-11-15T00:00:00", LastLoggedIn <= "2020-11-15T23:59:59" ) }""").body.toList() }
      // Jackson stream serialiser invokes 'close' on the stream which in turn inkoves the close handler attached in JdbcStreamingTemplate!
      val jsonString = mapper.writeValueAsString(stream)
      val response = mapper.readValue(jsonString, typeRef)
      response.should.have.size(1)
      response.map { it["firstName"] }.should.have.elements("Jack")
      connectionCountingDataSource.connectionList.all { it.isClosed }.should.be.`true`
   }

   @Test
   fun `can query date with zulu timezone information provided`() {
      val stream =
         runBlocking { service.submitVyneQlQuery("""find { Person[]( LastLoggedIn > "2020-11-15T00:00:00Z", LastLoggedIn <= "2020-11-15T23:59:59Z" ) }""").body.toList() }
      // Jackson stream serialiser invokes 'close' on the stream which in turn inkoves the close handler attached in JdbcStreamingTemplate!
      val jsonString = mapper.writeValueAsString(stream)
      val response = mapper.readValue(jsonString, typeRef)
      response.should.have.size(1)
      response.map { it["firstName"] }.should.have.elements("Jack")
      connectionCountingDataSource.connectionList.all { it.isClosed }.should.be.`true`
   }

}
