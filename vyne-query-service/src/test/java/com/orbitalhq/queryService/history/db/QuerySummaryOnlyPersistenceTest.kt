package com.orbitalhq.queryService.history.db

import app.cash.turbine.testIn
import com.jayway.awaitility.Awaitility
import com.orbitalhq.cockpit.core.ConfigService
import com.orbitalhq.cockpit.core.connectors.hazelcast.HazelcastHealthCheckProvider
import com.orbitalhq.copilot.CopilotConversationApi
import com.winterbe.expekt.should
import com.orbitalhq.history.db.QueryHistoryDbWriter
import com.orbitalhq.history.db.QueryHistoryRecordRepository
import com.orbitalhq.history.db.QueryResultRowRepository
import com.orbitalhq.history.rest.QueryHistoryService
import com.orbitalhq.licensing.LicenseManager
import com.orbitalhq.licensing.OrbitalLicenseManager
import com.orbitalhq.query.ResultMode
import com.orbitalhq.query.ValueWithTypeName
import com.orbitalhq.query.runtime.StreamResultStreamProvider
import com.orbitalhq.queryService.BaseQueryServiceTest
import com.orbitalhq.queryService.TestSpringConfig
import com.orbitalhq.schemaServer.core.editor.SchemaEditorService
import com.orbitalhq.schemaServer.core.packages.PackageService
import com.orbitalhq.schemaServer.core.repositories.WorkspaceConfigLoader
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectSpecLifecycleEventDispatcher
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ReactiveProjectStoreManager
import com.orbitalhq.schemas.fqn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.jose4j.jwk.RsaJwkGenerator
import org.jose4j.jws.AlgorithmIdentifiers
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwt.JwtClaims
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import java.util.*
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalCoroutinesApi
@RunWith(SpringRunner::class)
@ActiveProfiles("test")
@Import(TestSpringConfig::class)
@SpringBootTest(
   properties = [
      "vyne.schema.publicationMethod=LOCAL",
      "vyne.search.directory=./search/\${random.int}",
      "vyne.analytics.persistResults=false",
      "vyne.telemetry.enabled=false",
      "vyne.analytics.persistRemoteCallMetadata=false",
      "vyne.analytics.persistRemoteCallResponses=false",
      "spring.datasource.url=jdbc:h2:mem:testdbQuerySummaryOnlyPersistenceTest;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE;MODE=LEGACY"]
)
class QuerySummaryOnlyPersistenceTest : BaseQueryServiceTest() {
   companion object {
      @Container
      @ServiceConnection
      val postgres = PostgreSQLContainer<Nothing>("postgres:11.1").let {
         it.start()
         it.waitingFor(Wait.forListeningPort())
         it
      } as PostgreSQLContainer<*>

   }
   @MockitoBean
   lateinit var chatService: CopilotConversationApi

   @MockitoBean
   lateinit var streamResultStreamProvider: StreamResultStreamProvider

   @MockitoBean
   lateinit var reactiveProjectStoreManager: ReactiveProjectStoreManager

   @MockitoBean
   lateinit var configService: ConfigService
   @MockitoBean
   lateinit var licenseManager: OrbitalLicenseManager

   @MockitoBean
   lateinit var hazelcastHealthCheckProvider: HazelcastHealthCheckProvider

   @Autowired
   lateinit var historyDbWriter: QueryHistoryDbWriter

   @Autowired
   lateinit var queryHistoryRecordRepository: QueryHistoryRecordRepository

   @Autowired
   lateinit var resultRowRepository: QueryResultRowRepository

   @Autowired
   lateinit var historyService: QueryHistoryService

   @MockitoBean
   lateinit var eventDispatcher: ProjectSpecLifecycleEventDispatcher

   @MockitoBean
   lateinit var configLoader : WorkspaceConfigLoader

   @MockitoBean
   lateinit var packagesService: PackageService

   @MockitoBean
   lateinit var schemaEditorService: SchemaEditorService


   private fun authenticationWithPreferredUsername(preferredUsername: String): Authentication {
      val rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048)
      rsaJsonWebKey.apply {
         keyId = UUID.randomUUID().toString()
         algorithm = AlgorithmIdentifiers.RSA_USING_SHA256
         use = "sig"
      }
      val claims = JwtClaims().apply {
         jwtId = UUID.randomUUID().toString()
         issuer = "https://test.example.com"
         subject = UUID.randomUUID().toString()
         setExpirationTimeMinutesInTheFuture(10F)
         setIssuedAtToNow()
         setClaim("preferred_username", preferredUsername)
      }
      val jwt = JsonWebSignature().apply {
         payload = claims.toJson()
         key = rsaJsonWebKey.privateKey
         algorithmHeaderValue = rsaJsonWebKey.algorithm
         keyIdHeaderValue = rsaJsonWebKey.keyId
         setHeader("typ", "JWT")
      }.compactSerialization
      return JwtAuthenticationToken(
         NimbusReactiveJwtDecoder.withPublicKey(rsaJsonWebKey.getRsaPublicKey()).build().decode(jwt).block()
      )
   }

   @Test
   fun `username is persisted in query summary when query is submitted with authentication`() {
      setupTestService(historyDbWriter)
      val id = UUID.randomUUID().toString()
      val auth = authenticationWithPreferredUsername("marty.mcfly")

      runTest {
         val turbine = queryService.submitVyneQlQueryStreamingResponse(
            "find { Order[] } as Report[]",
            auth = auth,
            clientQueryId = id
         ).testIn(this)

         val first = turbine.awaitItem()
         first.should.not.be.`null`
         turbine.awaitComplete()
      }

      Awaitility.await().atMost(com.jayway.awaitility.Duration.TEN_SECONDS).until {
         queryHistoryRecordRepository.findByClientQueryId(id)?.endTime != null
      }

      val historyRecord = queryHistoryRecordRepository.findByClientQueryId(id)!!
      historyRecord.username.should.equal("marty.mcfly")
   }

   @Test
   fun `username is null in query summary when no authentication is provided`() {
      setupTestService(historyDbWriter)
      val id = UUID.randomUUID().toString()

      runTest {
         val turbine = queryService.submitVyneQlQueryStreamingResponse(
            "find { Order[] } as Report[]",
            auth = null,
            clientQueryId = id
         ).testIn(this)

         val first = turbine.awaitItem()
         first.should.not.be.`null`
         turbine.awaitComplete()
      }

      Awaitility.await().atMost(com.jayway.awaitility.Duration.TEN_SECONDS).until {
         queryHistoryRecordRepository.findByClientQueryId(id)?.endTime != null
      }

      val historyRecord = queryHistoryRecordRepository.findByClientQueryId(id)!!
      historyRecord.username.should.be.`null`
   }

   @Test
   fun `Only Query Summary is persisted when vyne history persistResults is false for a taxiQl query`() {
      setupTestService(historyDbWriter)
      val id = UUID.randomUUID().toString()

      runTest {
         val turbine =
            queryService.submitVyneQlQueryStreamingResponse("find { Order[] } as Report[]", clientQueryId = id).testIn(this)

         val first = turbine.awaitItem()
         first.should.not.be.`null`
         turbine.awaitComplete()
      }

      Awaitility.await().atMost(com.jayway.awaitility.Duration.TEN_SECONDS).until {
         val historyRecord = queryHistoryRecordRepository.findByClientQueryId(id)
         historyRecord!!.endTime != null
      }

      val historyRecord = queryHistoryRecordRepository.findByClientQueryId(id)

      historyRecord.should.not.be.`null`
      historyRecord!!.taxiQl.should.equal("find { Order[] } as Report[]")
      historyRecord.endTime.should.not.be.`null`
      historyRecord.recordCount.should.equal(1)

      val results = resultRowRepository.findAllByQueryId(id)

      results.should.be.empty

      val historyProfileData = historyService.getQueryProfileDataFromClientId(id)
      historyProfileData.block()!!.remoteCalls.should.be.empty
   }

   @Test
   fun `Only Query Summary is persisted when vyne history persistResults is false for a query`() {
      setupTestService(historyDbWriter)
      val id = UUID.randomUUID().toString()

      runBlocking {
         val turbine =
            queryService.submitVyneQlQueryStreamingResponse("""find { Order[] }""", ResultMode.TYPED, MediaType.APPLICATION_JSON_VALUE, clientQueryId = id).testIn(this)
         val next = turbine.awaitItem() as ValueWithTypeName
         next.typeName.should.equal("Order".fqn().parameterizedName)
         turbine.awaitComplete()
      }

      Awaitility.await().atMost(com.jayway.awaitility.Duration.TEN_SECONDS).until {
         val historyRecord = queryHistoryRecordRepository.findByClientQueryId(id)
         historyRecord!!.endTime != null
      }

      val historyRecord = queryHistoryRecordRepository.findByClientQueryId(id)!!

      historyRecord.should.not.be.`null`
      historyRecord.taxiQl.should.not.be.`null`
      historyRecord.endTime.should.not.be.`null`
      historyRecord.recordCount.should.equal(1)

      val results = resultRowRepository.findAllByQueryId(id)

      results.should.be.empty

      val historyProfileData = historyService.getQueryProfileDataFromClientId(id)
      historyProfileData.block()!!.remoteCalls.should.be.empty
   }
}
