package io.vyne.pipelines.jet.source.query

import com.winterbe.expekt.should
import io.vyne.models.json.parseJson
import io.vyne.pipelines.jet.BaseJetIntegrationTest
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.TypedInstanceContentProvider
import io.vyne.pipelines.jet.api.transport.http.CronExpressions
import io.vyne.pipelines.jet.api.transport.query.PollingQueryInputSpec
import kotlinx.coroutines.flow.flowOf
import org.awaitility.Awaitility
import org.junit.Test
import java.time.Duration
import java.util.concurrent.TimeUnit

class PollingQuerySourceBuilderTest : BaseJetIntegrationTest() {
   @Test
   fun `poll the query and return values`() {
      val (jetInstance, applicationContext, vyneProvider, stub) = jetWithSpringAndVyne(
         """
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
         service PeopleService {
            operation getAll(): Person[]
         }
      """
      )

      val vyne = vyneProvider.createVyne()
      stub.addResponseFlow("getAll") { _, _ ->
         flowOf(
            vyne.parseJson("Person", """{ "firstName": "Jimmy", "lastName": "Fallon" }"""),
            vyne.parseJson("Person", """{ "firstName": "Conan", "lastName": "O'Brien" }"""),
            vyne.parseJson("Person", """{ "firstName": "Jimmy", "lastName": "Kimmel" }"""),
         )
      }

      val query = """
         find { Person[] }
      """

      val (listSinkTarget, outputSpec) = listSinkTargetAndSpec(applicationContext, targetType = "Person")
      val pipelineSpec = PipelineSpec(
         name = "test-query-poll",
         input = PollingQueryInputSpec(
            query,
            CronExpressions.EVERY_SECOND
         ),
         outputs = listOf(outputSpec)
      )

      startPipeline(jetInstance, vyneProvider, pipelineSpec)

      applicationContext.moveTimeForward(Duration.ofSeconds(2))
      Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
         listSinkTarget.list.size == 3
      }

      val outputValue = listSinkTarget.list.map { (it as TypedInstanceContentProvider).content.toRawObject() }
      outputValue.toSet().should.equal(
         setOf(
            mapOf("firstName" to "Jimmy", "lastName" to "Fallon"),
            mapOf("firstName" to "Conan", "lastName" to "O'Brien"),
            mapOf("firstName" to "Jimmy", "lastName" to "Kimmel"),
         )
      )
   }
}

