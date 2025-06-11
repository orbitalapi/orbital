package com.orbitalhq.queryService

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.test.TestHazelcastInstanceFactory
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.AuthClaimType
import com.orbitalhq.UserType.UsernameTypeDefinition
import com.orbitalhq.Vyne
import com.orbitalhq.VyneProvider
import com.orbitalhq.errors.ErrorType
import com.orbitalhq.formats.csv.CsvFormatSpec
import com.orbitalhq.history.db.QueryHistoryDbWriter
import com.orbitalhq.metrics.NoOpMetricsReporter
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.models.json.parseJsonModel
import com.orbitalhq.models.json.parseKeyValuePair
import com.orbitalhq.query.HistoryEventConsumerProvider
import com.orbitalhq.query.QueryEventConsumer
import com.orbitalhq.query.runtime.core.QueryLifecycleEventObserver
import com.orbitalhq.query.runtime.core.QueryResponseFormatter
import com.orbitalhq.query.runtime.core.QueryService
import com.orbitalhq.query.runtime.core.monitor.ActiveQueryMonitor
import com.orbitalhq.query.tracing.NoopTracingEventSink
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.spring.SimpleVyneProvider
import com.orbitalhq.spring.config.TestDiscoveryClientConfig
import com.orbitalhq.stubbing.StubService
import com.orbitalhq.testVyne
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.jose4j.jwk.RsaJwkGenerator
import org.jose4j.jws.AlgorithmIdentifiers
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwt.JwtClaims
import org.junit.jupiter.api.BeforeAll
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.ByteArrayOutputStream
import java.util.UUID

@Testcontainers
abstract class BaseQueryServiceTest {
   companion object {
      @Container
      @ServiceConnection
      val postgres = PostgreSQLContainer<Nothing>("postgres:11.1") as PostgreSQLContainer<*>

      @BeforeAll
      fun setup() {
         postgres.start()
         postgres.waitingFor(Wait.forListeningPort())
      }

      val testSchema = """
         type OrderId inherits String
         type TraderName inherits String
         type InstrumentId inherits String
         type MaturityDate inherits Date
         type InstrumentMaturityDate inherits MaturityDate
         type TradeMaturityDate inherits MaturityDate
         type TradeId inherits String
         type InstrumentName inherits String
         type EmptyId inherits String
         type Role inherits String

         model Order {
            orderId: OrderId
            traderName : TraderName
            instrumentId: InstrumentId
         }
         model Instrument {
             instrumentId: InstrumentId
             maturityDate: InstrumentMaturityDate
             name: InstrumentName
         }
         model Trade {
            orderId: OrderId
            maturityDate: TradeMaturityDate
            tradeId: TradeId
         }

         model Report {
            orderId: OrderId
            tradeId: TradeId
            instrumentName: InstrumentName
            @FirstNotEmpty
            maturityDate: MaturityDate
            traderName : TraderName
         }

         model Empty {
            id: EmptyId
         }

         model Client {
            clientId: ClientId inherits String
            clientName: ClientName inherits String
         }

         model UserInfo inherits com.orbitalhq.auth.AuthClaims {
            roles: Role[]
         }

         policy AdminRestrictedClients against Client  (userInfo : UserInfo) -> {
            read {
               when {
                  userInfo.roles.contains('Admin') -> Client
                  else -> throw((NotAuthorizedError) { message: 'Not Authorized' })
               }
            }
         }

         service MultipleInvocationService {
            operation getOrders(): Order[]
            operation getTrades(orderIds: OrderId): Trade
            operation getTrades(orderIds: OrderId[]): Trade[]
            operation getInstrument(instrumentId: InstrumentId): Instrument
            operation getClients(): Client[]
         }


      """.trimIndent()
   }

   lateinit var queryService: QueryService
   lateinit var stubService: StubService
   lateinit var vyne: Vyne
   lateinit var queryEventObserver: QueryLifecycleEventObserver
   lateinit var meterRegistry: SimpleMeterRegistry


   protected fun mockHistoryWriter(): QueryHistoryDbWriter {
      val eventConsumer: QueryEventConsumer = mock {}
      val historyWriter: QueryHistoryDbWriter = mock {
         on { createEventConsumer(any(), any()) } doReturn eventConsumer
      }
      return historyWriter
   }

   protected fun setupTestService(
      historyDbWriter: QueryHistoryDbWriter = mockHistoryWriter(),
      schema: String = testSchema,
      prepareStubCallback: (StubService, Vyne) -> Unit = { stub, vyne -> this.prepareStubService(stub, vyne) }
   ) {
      val (vyne, stubService) = testVyne(schema, AuthClaimType.AuthClaimsTypeDefinition, ErrorType.ErrorTypeDefinition, UsernameTypeDefinition)
      setupTestService(vyne, stubService, historyDbWriter)
      prepareStubCallback(stubService, vyne)
   }

   protected fun setupTestService(
      vyne: Vyne,
      stubService: StubService?,
      historyDbWriter: HistoryEventConsumerProvider = mockHistoryWriter()
   ): QueryService {
      if (stubService != null) {
         this.stubService = stubService
      }
      this.vyne = vyne
      this.meterRegistry = SimpleMeterRegistry()

      queryService = QueryService(
         SimpleSchemaProvider(vyne.schema),
         SimpleVyneProvider(vyne),
         historyDbWriter,
         Jackson2ObjectMapperBuilder().build(),
         ActiveQueryMonitor(TestHazelcastInstanceFactory().newHazelcastInstance()),
         QueryResponseFormatter(listOf(CsvFormatSpec)),
         NoopTracingEventSink
      )
      return queryService
   }

   protected fun authenticationWithRoles(roles: List<String>): Authentication {
      val rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048)
      rsaJsonWebKey.apply {
         keyId = UUID.randomUUID().toString()
         algorithm = AlgorithmIdentifiers.RSA_USING_SHA256
         use = "sig"
      }
      val claims = JwtClaims().apply {
         jwtId = UUID.randomUUID().toString() // unique identifier for the JWT
         issuer = "https://login.microsoftonline.com/0592515b-94ca-42b2-ad9b-65bfb2104867/v2.0" // identifies the principal that issued the JWT
         subject = "4f76ac09-a587-48ec-a22a-43060003ba2f" // identifies the principal that is the subject of the JWT
         setAudience("https://host/api") // identifies the recipients that the JWT is intended for
         setExpirationTimeMinutesInTheFuture(10F) // identifies the expiration time on or after which the JWT MUST NOT be accepted for processing
         setIssuedAtToNow() // identifies the time at which the JWT was issued
         setClaim("azp", "example-client-id") // Authorized party - the party to which the ID Token was issued
         setClaim("scope", "openid profile email") // Scope Values
         setClaim("roles", roles)
      }

      val clientCredentialsBearerToken = JsonWebSignature().apply {
         payload = claims.toJson()
         key = rsaJsonWebKey.privateKey // the key to sign the JWS with
         algorithmHeaderValue = rsaJsonWebKey.algorithm // Set the signature algorithm on the JWT/JWS that will integrity protect the claims
         keyIdHeaderValue = rsaJsonWebKey.keyId // a hint indicating which key was used to secure the JWS
         setHeader("typ", "JWT") // the media type of this JWS
      }.compactSerialization


      return JwtAuthenticationToken( NimbusReactiveJwtDecoder.withPublicKey(rsaJsonWebKey.getRsaPublicKey()).build().decode(clientCredentialsBearerToken).block())



   }

   public fun prepareStubService(stubService: StubService, vyne: Vyne) {
      stubService.addResponse(
         "getOrders", vyne.parseJsonModel(
            "Order[]", """
            [
               {
                  "orderId": "orderId_0",
                  "traderName": "john",
                  "instrumentId": "Instrument_0"
               }
            ]
            """.trimIndent()
         ), modifyDataSource = true
      )

      val maturityDateTrade = "2026-12-01"
      val response = vyne.parseJsonModel(
         "Trade[]", """
               [{
                  "maturityDate": "$maturityDateTrade",
                  "orderId": "orderId_0",
                  "tradeId": "Trade_0"
               }]
            """.trimIndent()
      )
      stubService.addResponse(
         "getTrades", response, modifyDataSource = true
      )

      stubService.addResponse(
         "getInstrument", vyne.parseJsonModel(
            "Instrument", """
               {
                  "instrumentId": "Instrument_0",
                  "name": "2040-11-20 0.1 Bond"
               }
            """.trimIndent()
         ), modifyDataSource = true
      )
   }
}

fun ResponseEntity<StreamingResponseBody>.contentString(): String {
   val stream = ByteArrayOutputStream()
   this.body!!.writeTo(stream)
   return String(stream.toByteArray())
}

@TestConfiguration
@Import(TestDiscoveryClientConfig::class)
class TestSpringConfig {

   @Bean
   fun taxiSchema(): TaxiSchema = TaxiSchema.from(
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

   @Bean
   fun metricsReporter() = NoOpMetricsReporter

   @Bean
   fun hazelcastInstance(): HazelcastInstance = MockHazelcastInstance()

   @Bean
   @Primary
   fun vyneProvider(taxiSchema: TaxiSchema): VyneProvider {
      val (vyne, stub) = testVyne(taxiSchema)
      // setup stubs
      stub.addResponse(
         "findPersonIdByEmail",
         TypedInstance.from(vyne.type("PersonId"), 1, vyne.schema),
         modifyDataSource = true
      )
      stub.addResponse(
         "findMembership",
         vyne.parseKeyValuePair("LoyaltyCardNumber", "1234-5678"),
         modifyDataSource = true
      )
      stub.addResponse(
         "findBalance",
         vyne.parseJson("AccountBalance", """{ "balance" : 100 }"""),
         modifyDataSource = true
      )
      return SimpleVyneProvider(vyne)
   }
}
