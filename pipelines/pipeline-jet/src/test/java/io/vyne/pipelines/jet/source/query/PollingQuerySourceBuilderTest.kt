package io.vyne.pipelines.jet.source.query

import com.winterbe.expekt.should
import io.vyne.models.json.parseJson
import io.vyne.pipelines.jet.BaseJetIntegrationTest
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.TypedInstanceContentProvider
import io.vyne.pipelines.jet.api.transport.http.CronExpressions
import io.vyne.pipelines.jet.api.transport.query.PollingQueryInputSpec
import io.vyne.pipelines.jet.pipelines.PipelineManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import nl.altindag.log.LogCaptor
import org.awaitility.Awaitility
import org.junit.Test
import java.util.concurrent.TimeUnit

class PollingQuerySourceBuilderTest : BaseJetIntegrationTest() {
   @Test
   fun `poll the query and return values`() {
      val testSetup = jetWithSpringAndVyne(
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

      testSetup.stubService.addResponseFlow("getAll") { _, _ ->
         flowOf(
            parseJson(testSetup.schema, "Person", """{ "firstName": "Jimmy", "lastName": "Fallon" }"""),
            parseJson(testSetup.schema, "Person", """{ "firstName": "Conan", "lastName": "O'Brien" }"""),
            parseJson(testSetup.schema, "Person", """{ "firstName": "Jimmy", "lastName": "Kimmel" }"""),
         )
      }

      val query = """
         find { Person[] }
      """

      val (listSinkTarget, outputSpec) = listSinkTargetAndSpec(testSetup.applicationContext, targetType = "Person")
      val pipelineSpec = PipelineSpec(
         name = "test-query-poll",
         input = PollingQueryInputSpec(query, CronExpressions.EVERY_SECOND),
         outputs = listOf(outputSpec)
      )

      startPipeline(testSetup.hazelcastInstance, testSetup.vyneClient, pipelineSpec)
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

   @Test
   fun `poll the query with preventConcurrentExecution delay`() {
      val pipelineManagerLogCaptor = LogCaptor.forClass(PipelineManager::class.java)
      pipelineManagerLogCaptor.setLogLevelToTrace()
      val testSetup = jetWithSpringAndVyne(
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

      testSetup.stubService.addResponseFlow("getAll") { _, _ ->
         flow {
            delay(10000L)
            emit(parseJson(testSetup.schema, "Person", """{ "firstName": "Jimmy", "lastName": "Fallon" }"""))
            emit(parseJson(testSetup.schema, "Person", """{ "firstName": "Conan", "lastName": "O'Brien" }"""))
            emit(parseJson(testSetup.schema, "Person", """{ "firstName": "Jimmy", "lastName": "Kimmel" }"""))
         }
      }

      val query = """
         find { Person[] }
      """

      val (listSinkTarget, outputSpec) = listSinkTargetAndSpec(testSetup.applicationContext, targetType = "Person")
      val pipelineSpec = PipelineSpec(
         name = "test-query-poll",
         input = PollingQueryInputSpec(
            query, CronExpressions.EVERY_SECOND,
            preventConcurrentExecution = true
         ),
         outputs = listOf(outputSpec)
      )

      startPipeline(testSetup.hazelcastInstance, testSetup.vyneClient, pipelineSpec)
      Awaitility.await().atMost(30, TimeUnit.SECONDS).until {
         listSinkTarget.list.size == 3 && pipelineManagerLogCaptor.traceLogs.any { traceLog ->
            traceLog.contains("Skipping pipeline \"test-query-poll\" as it is input spec set as fixedDelay, and there is an active job")
         }
      }
   }
}

