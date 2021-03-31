package io.vyne.cask.query.vyneql

import io.vyne.cask.query.BaseCaskIntegrationTest
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import reactor.test.StepVerifier

class StreamingVyneQlQueryServiceTest : BaseCaskIntegrationTest() {
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

   @Before
   override fun setup() {
      super.setup()
      schemaProvider.updateSource(schema)
      val schema = schemaProvider.schema() as TaxiSchema
      val person = schema.versionedType("Person".fqn())
      service = VyneQlQueryService(jdbcStreamingTemplate, VyneQlSqlGenerator(
         schemaProvider, configRepository
      ))
      val json =
         """[
         { "firstName" : "Jimmy", "age" : 35, "lastLogin" : "2020-11-16T11:47:00Z" },
         { "firstName" : "Jack", "age" : 32, "lastLogin" : "2020-11-15T11:47:00Z" },
         { "firstName" : "John", "age" : 55, "lastLogin" : "2020-10-15T11:47:00Z" }]"""

      ingestJsonData(json, person, schema)
   }

   @Test
   fun findMatchingNoCriteriaReturnsEmptyList() {
      val response = runBlocking { service.submitVyneQlQueryStreamingResponse("""findAll { Person[]( FirstName = "Nobody" ) }""").body}
      StepVerifier.create(response).verifyComplete()
   }

   @Test
   fun canExecuteFindAll() {
      val response = runBlocking { service.submitVyneQlQueryStreamingResponse("""findAll { Person[] }""").body}
      StepVerifier.create(response.map { mapOf("firstName" to it["firstName"]) })
         .expectNextCount(3)
         .verifyComplete()

   }
   @Test
   fun `can find with greater than number`() {
      val response = runBlocking {service.submitVyneQlQueryStreamingResponse("""findAll { Person[]( Age > 33 ) }""").body}
      StepVerifier.create(response.map { mapOf("firstName" to it["firstName"]) })
         .expectNextCount(2)
         .verifyComplete()

   }
   @Test
   fun `can find with greater then or equal number`() {
      val response = runBlocking {service.submitVyneQlQueryStreamingResponse("""findAll { Person[]( Age >= 35 ) }""").body}
      StepVerifier.create(response.map { mapOf("firstName" to it["firstName"]) })
         .expectNextCount(2)
         .verifyComplete()
   }
   @Test
   fun `can find with less than number`() {
      val response = runBlocking {service.submitVyneQlQueryStreamingResponse("""findAll { Person[]( Age < 33 ) }""").body}
      StepVerifier.create(response.map { mapOf("firstName" to it["firstName"]) })
         .expectNext(mapOf("firstName" to "Jack"))
         .verifyComplete()
   }
   @Test
   fun `can find with less or equal than number`() {
      val response = runBlocking {service.submitVyneQlQueryStreamingResponse("""findAll { Person[]( Age <= 35 ) }""").body}
      StepVerifier.create(response.map { mapOf("firstName" to it["firstName"]) })
         .expectNextCount(2)
         .verifyComplete()
   }

   @Test
   fun `can find with date after`() {
      val response = runBlocking {service.submitVyneQlQueryStreamingResponse("""findAll { Person[]( LastLoggedIn > '2020-11-01T00:00:00Z' ) }""").body}
      StepVerifier.create(response.map { mapOf("firstName" to it["firstName"]) })
         .expectNextCount(2)
         .verifyComplete()
   }
   @Test
   fun `can find with date between`() {
      val response = runBlocking {service.submitVyneQlQueryStreamingResponse("""findAll { Person[]( LastLoggedIn > "2020-11-15T00:00:00Z", LastLoggedIn <= "2020-11-15T23:59:59Z" ) }""").body}
      StepVerifier.create(response.map { mapOf("firstName" to it["firstName"]) })
         .expectNext(mapOf("firstName" to "Jack"))
         .verifyComplete()
   }
   @Test
   fun `can query with an abstract property type`() {
      val response = runBlocking {service.submitVyneQlQueryStreamingResponse("""findAll { Person[]( LoginTime > "2020-11-15T00:00:00", LoginTime <= "2020-11-15T23:59:59" ) }""").body}
      StepVerifier.create(response.map { mapOf("firstName" to it["firstName"]) })
         .expectNext(mapOf("firstName" to "Jack"))
         .verifyComplete()
   }

   @Test
   fun `can query date without timezone information provided`() {
      val response = runBlocking {service.submitVyneQlQueryStreamingResponse("""findAll { Person[]( LastLoggedIn > "2020-11-15T00:00:00", LastLoggedIn <= "2020-11-15T23:59:59" ) }""").body}
      StepVerifier.create(response.map { mapOf("firstName" to it["firstName"]) })
         .expectNext(mapOf("firstName" to "Jack"))
         .verifyComplete()
   }

   @Test
   fun `can query date with zulu timezone information provided`() {
      val response = runBlocking {service.submitVyneQlQueryStreamingResponse("""findAll { Person[]( LastLoggedIn > "2020-11-15T00:00:00Z", LastLoggedIn <= "2020-11-15T23:59:59Z" ) }""").body }
      StepVerifier.create(response.map { mapOf("firstName" to it["firstName"]) })
         .expectNext(mapOf("firstName" to "Jack"))
         .verifyComplete()

   }

}