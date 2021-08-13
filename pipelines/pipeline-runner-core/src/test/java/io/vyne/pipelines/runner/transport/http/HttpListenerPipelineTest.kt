package io.vyne.pipelines.runner.transport.http

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jayway.awaitility.Awaitility.await
import com.netflix.appinfo.ApplicationInfoManager
import com.winterbe.expekt.should
import io.vyne.VersionedTypeReference
import io.vyne.annotations.http.HttpOperations
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.PipelineChannel
import io.vyne.pipelines.orchestrator.events.PipelineEventsApi
import io.vyne.pipelines.runner.PipelineRunnerTestApp
import io.vyne.pipelines.runner.jobs.PipelineStateManager
import io.vyne.pipelines.runner.transport.direct.DirectOutputBuilder
import io.vyne.pipelines.runner.transport.direct.DirectOutputSpec
import io.vyne.schemas.fqn
import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.test.context.junit4.SpringRunner
import java.util.concurrent.TimeUnit

@RunWith(SpringRunner::class)
@SpringBootTest(
   classes = [PipelineRunnerTestApp::class],
   webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
   properties = [
      "spring.main.allow-bean-definition-overriding=true",
      "eureka.client.enabled=false",
      "vyne.schema.publicationMethod=LOCAL",
      "spring.cloud.config.enabled= false",
      "spring.main.web-application-type=reactive",
      "spring.application.name=test-pipeline-runner",
      "vyne.caskService.name=CASK"
   ]
)
class HttpListenerPipelineTest {
   @LocalServerPort
   val webServerPort = 0

   @Autowired
   lateinit var stateManager: PipelineStateManager

   @MockBean
   lateinit var client: DiscoveryClient;

   @MockBean
   lateinit var applicationInfoManager: ApplicationInfoManager

   @MockBean
   lateinit var pipelineEventApi: PipelineEventsApi

   @Autowired
   lateinit var directOutputBuilder: DirectOutputBuilder

   @Before
   fun setup() {
      directOutputBuilder.clearAll()
   }

   @Test
   fun `submit pipeline that creates http listener`() {
      val pipeline = Pipeline(
         name = "http listening pipeline",
         input = PipelineChannel(
            HttpListenerTransportSpec(
               path = "/test/foo",
               method = HttpOperations.HttpMethod.POST,
               payloadType = VersionedTypeReference("PersonLoggedOnEvent".fqn())
            )
         ),
         output = PipelineChannel(
            DirectOutputSpec(messageType = VersionedTypeReference("PersonLoggedOnEvent".fqn()))
         )
      )
      val instance = stateManager.registerPipeline(pipeline)
      await().atMost(10, TimeUnit.SECONDS).until<Boolean> { instance.isHealthy }
      pushTrigger("/test/foo", mapOf("Hello" to "World"))

      await().atMost(10, TimeUnit.SECONDS).until<Boolean> {
         val output = directOutputBuilder.builtInstances.first()
         output.messages.size > 0
      }
      val firstMessage = directOutputBuilder.builtInstances.first().messages.first()
      JSONAssert.assertEquals(
         """
         { "Hello" : "World" }
      """, firstMessage, true
      )
   }

   @Test
   fun `when submitted payload does not match contract then it is rejected with bad request`() {

   }

   private fun pushTrigger(path: String, message: Map<String, Any>) {
      val client = ApacheClient()
      val json = jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(message)
      val request =
         org.http4k.core.Request(
            method = Method.POST,
            uri = "http://localhost:$webServerPort/api/triggers/${path.removePrefix("/")}"
         )
            .body(json).headers(
               listOf(Pair("Content-Type", "application/json"))
            )

      val response = client.invoke(request)
      response.status.code.should.equal(200)
   }
}
