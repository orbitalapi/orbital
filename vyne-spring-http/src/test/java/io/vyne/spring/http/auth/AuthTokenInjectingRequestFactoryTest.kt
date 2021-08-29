package io.vyne.spring.http.auth

import com.winterbe.expekt.should
import io.vyne.models.TypedInstance
import io.vyne.schemas.OperationNames
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.spring.http.DefaultRequestFactory
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.springframework.http.HttpHeaders

class AuthTokenInjectingRequestFactoryTest {

   @Rule
   @JvmField
   val folder = TemporaryFolder()

   val schema = TaxiSchema.from("""
         model Person {
            id : PersonId inherits String
         }
         type Postcode inherits String
         model Street
         service PersonService {
            operation findPerson(@RequestBody PersonId):Person
         }
         service StreetService {
            operation listAllStreets(@RequestBody Postcode): Street[]
         }
      """.trimIndent())

   val authConfig = """
      authenticationTokens {
         PersonService {
            tokenType = AuthorizationBearerHeader
            value = "abcd1234"
         }
      }
   """.trimIndent()

   @Test
   fun `when auth token is configured for service then the header is injected`() {
      val authConfigFile = folder.root.resolve("auth.conf")
      authConfigFile.writeText(authConfig)
      val requestFactory = AuthTokenInjectingRequestFactory(
         DefaultRequestFactory(),
         ConfigFileAuthTokenRepository(authConfigFile.toPath())
      )

      val operationName = OperationNames.qualifiedName("PersonService", "findPerson")
      val request = requestFactory.buildRequestBody(
         schema.operation(operationName).second,
         listOf(TypedInstance.from(schema.type("PersonId"), "Person1", schema))
      )
      request.body.should.equal("Person1")
      val authHeaders = request.headers.get(HttpHeaders.AUTHORIZATION)!!
      authHeaders.should.have.size(1)
      authHeaders.first().should.equal("Bearer abcd1234")
   }

   @Test
   fun `when no auth token is configured for a service then no headers are added`() {
      val authConfigFile = folder.root.resolve("auth.conf")
      authConfigFile.writeText(authConfig)
      val requestFactory = AuthTokenInjectingRequestFactory(
         DefaultRequestFactory(),
         ConfigFileAuthTokenRepository(authConfigFile.toPath())
      )

      val operationName = OperationNames.qualifiedName("StreetService", "listAllStreets")
      val request = requestFactory.buildRequestBody(
         schema.operation(operationName).second,
         listOf(TypedInstance.from(schema.type("Postcode"), "SW11", schema))
      )
      request.body.should.equal("SW11")
      val authHeaders = request.headers.get(HttpHeaders.AUTHORIZATION)
      authHeaders.should.be.`null`
   }
}
