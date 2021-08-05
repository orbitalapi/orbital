package io.vyne.pipelines.runner.transport.http

import com.jayway.awaitility.Awaitility.await
import com.winterbe.expekt.should
import io.vyne.StubService
import io.vyne.VersionedTypeReference
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.models.TypedObject
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.PipelineChannel
import io.vyne.pipelines.PipelineInputMessage
import io.vyne.pipelines.StringContentProvider
import io.vyne.pipelines.runner.PipelineRunnerTestApp
import io.vyne.pipelines.runner.jobs.PipelineStateManager
import io.vyne.pipelines.runner.transport.direct.DirectTransportInputSpec
import io.vyne.schemas.OperationNames
import io.vyne.schemas.Schema
import io.vyne.schemas.fqn
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.test.context.junit4.SpringRunner
import reactor.core.publisher.Sinks
import java.time.Instant
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
      "spring.application.name=test-pipeline-runner",
      "vyne.caskService.name=CASK"
   ]
)
class TaxiOperationTargetPipelineTest {

   @Autowired
   lateinit var stateManager: PipelineStateManager

   @MockBean
   lateinit var client: DiscoveryClient;

   @Autowired
   lateinit var stubService: StubService

   @Autowired
   lateinit var schema:Schema

   @Before
   fun setup() {
      stubService.clearAll()
   }

   @Test
   fun `can submit pipeline that publishes to taxi operation`() {
      val messageSink = Sinks.many().unicast().onBackpressureError<PipelineInputMessage>()
      val pipeline = Pipeline(
         name = "direct input",
         input = PipelineChannel(
            DirectTransportInputSpec(
               source = messageSink.asFlux(),
               messageType = VersionedTypeReference("PersonLoggedOnEvent".fqn())
            )
         ),
         output = PipelineChannel(
            TaxiOperationOutputSpec(operationName = OperationNames.name("UserService","trackUserEvent"))
         )
      )
      // Create the pipeline, and wait for it to be ready
      val instance = stateManager.registerPipeline(pipeline)
      await().atMost(10, TimeUnit.SECONDS).until<Boolean> { instance.isHealthy }

      // Prep the stub
      stubService.addResponse("getUserNameFromId", TypedInstance.from(schema.type("Username"), "Jimmy Schmitts", schema = schema))
      stubService.addResponse("trackUserEvent", TypedNull.create(schema.type("lang.taxi.String")))

      // Send a message on the input sink
      val emitResult = messageSink.tryEmitNext(
         PipelineInputMessage(
            Instant.now(),
            emptyMap(),
            StringContentProvider("""{ "userId" : "jimmy" }""")
         )
      )
      emitResult.isSuccess.should.be.`true`

      await().atMost(10, TimeUnit.SECONDS).until<Boolean> { stubService.invocations["trackUserEvent"]?.size == 1 }
      val requestPayload = stubService.invocations["trackUserEvent"]!!.first() as TypedObject
      requestPayload.toRawObject().should.equal(mapOf(
         "id" to "jimmy",
         "name" to "Jimmy Schmitts"
      ))
   }

   @Test
   fun `it is invalid to create a taxi operation target that takes multiple inputs`() {
      TODO()
   }
}
