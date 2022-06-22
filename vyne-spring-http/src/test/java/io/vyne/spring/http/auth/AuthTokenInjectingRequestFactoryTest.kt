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
         service BasicPersonService {
            operation findBasicPerson(@RequestBody PersonId):Person
         }
         service ApiKeyPersonService {
            operation findApiKeyPerson(@RequestBody PersonId):Person
         }
         service CookiePersonService {
            operation findCookiePerson(@RequestBody PersonId):Person
         }
      """.trimIndent())

   val authConfig = """
      authenticationTokens {
         PersonService {
            tokenType = Header
            paramName = Authorization
            valuePrefix = Bearer
            value = "abcd1234"
         }

         BasicPersonService {
            tokenType = Header
            paramName = Authorization
            valuePrefix = Basic
            value = "AXVubzpwQDU1dzByYM=="
         }

         ApiKeyPersonService {
            tokenType = QueryParam
            paramName = api_key
            value = "abcdefgh123456789"
         }

         CookiePersonService {
            tokenType = Cookie
            paramName = api_key
            value = "abcdefgh123456789"
         }
      }
   """.trimIndent()

   @Test
   fun `when basic auth token is configured for service then the header is injected`() {
      val authConfigFile = folder.root.resolve("auth.conf")
      authConfigFile.writeText(authConfig)
      val requestFactory = AuthTokenInjectingRequestFactory(
         DefaultRequestFactory(),
         ConfigFileAuthTokenRepository(authConfigFile.toPath())
      )

      val operationName = OperationNames.qualifiedName("BasicPersonService", "findBasicPerson")
      val request = requestFactory.buildRequestBody(
         schema.operation(operationName).second,
         listOf(TypedInstance.from(schema.type("PersonId"), "Person1", schema))
      )
      request.body.should.equal("Person1")
      val authHeaders = request.headers[HttpHeaders.AUTHORIZATION]!!
      authHeaders.should.have.size(1)
      authHeaders.first().should.equal("Basic AXVubzpwQDU1dzByYM==")
   }

   @Test
   fun `when bearer token auth credentials is configured for service then the header is injected`() {
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
      val authHeaders = request.headers[HttpHeaders.AUTHORIZATION]!!
      authHeaders.should.have.size(1)
      authHeaders.first().should.equal("Bearer abcd1234")
   }

   @Test
   fun `when query param api key is configured for service then the query param is injected`() {
      val authConfigFile = folder.root.resolve("auth.conf")
      authConfigFile.writeText(authConfig)
      val requestFactory = AuthTokenInjectingRequestFactory(
         DefaultRequestFactory(),
         ConfigFileAuthTokenRepository(authConfigFile.toPath())
      )

      val operationName = OperationNames.qualifiedName("ApiKeyPersonService", "findApiKeyPerson")
      val request = requestFactory.buildRequestBody(
         schema.operation(operationName).second,
         listOf(TypedInstance.from(schema.type("PersonId"), "Person1", schema))
      )
      request.body.should.equal("Person1")
      request.headers[HttpHeaders.AUTHORIZATION].should.be.`null`
      val queryParams = requestFactory.buildRequestQueryParams(schema.operation(operationName).second)
      queryParams!!["api_key"]!![0].should.equal("abcdefgh123456789")
   }

   @Test
   fun `when cookie is configured for service then the cookie is injected`() {
      val authConfigFile = folder.root.resolve("auth.conf")
      authConfigFile.writeText(authConfig)
      val requestFactory = AuthTokenInjectingRequestFactory(
         DefaultRequestFactory(),
         ConfigFileAuthTokenRepository(authConfigFile.toPath())
      )

      val operationName = OperationNames.qualifiedName("CookiePersonService", "findCookiePerson")
      val request = requestFactory.buildRequestBody(
         schema.operation(operationName).second,
         listOf(TypedInstance.from(schema.type("PersonId"), "Person1", schema))
      )
      request.body.should.equal("Person1")
      val cookies = request.headers[HttpHeaders.COOKIE]
      cookies.should.not.be.`null`
      cookies!!.first().should.equal("api_key=abcdefgh123456789")
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
