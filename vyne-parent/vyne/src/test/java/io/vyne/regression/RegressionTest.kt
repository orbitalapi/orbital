package io.vyne.regression

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.io.Resources
import io.vyne.Vyne
import io.vyne.models.TypeNamedInstance
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.query.*
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.log
import org.junit.Test
import java.io.File
import java.time.Instant
import kotlin.test.fail

/**
 * This class replays the tests found in resources/scnearios
 * These are captured outputs from the Vyne UI, which we then replay to ensure
 * no regressions.
 *
 * Tests exist in a directory containing a schema.json, and then a bunch of test scneario,
 * also named xxxx.json
 *
 * To add a test:
 * Run Vyne and the services you want to test
 * Execute the scenario within the UI
 * With the Network tab of chrome devtools open, Visit the schema explorer, and grab the response of /schema
 * Save this response in a directory, and name the file schema.json
 *
 * Copy sampleSpec.json into your directory, change the test name and background to describe your test
 * Then, visit the query history tab, and grab the response of /history
 *
 * Pluck the value from the output that matches the scneario you want to replay, and paste it where indicated.
 *
 * Bosh.
 *
 * Find the entry you wan
 */
class RegressionTest {

   private val objectMapper: ObjectMapper = jacksonObjectMapper()
      .registerModule(VyneJacksonModule())
      .registerModule(JavaTimeModule())
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

   @Test
   fun runTest() {
      val resource = Resources.getResource("scenarios")
      val root = File(resource.toURI())
      root.walkTopDown().forEach {
         val schemaFile = it.toPath().resolve("schema.json").toFile()
         if (it.isDirectory && schemaFile.exists()) {
            val schema = schemaFile.readText()
            executeTestsInDirectory(schema, it)
         }
      }
   }

   private fun executeTestsInDirectory(schemaJson: String, testDirectory: File) {

      val schemas = objectMapper.readValue<List<VersionedSchema>>(schemaJson)
      val testFiles = testDirectory.listFiles { file ->
         file.name != "schema.json" && file.extension == "json"
      }

      val testExecutions = testFiles.map { testFile ->
         val testCase = objectMapper.readValue<VyneTestCase>(testFile)
         testCase to executeTestScenario(schemas, testCase)
      }

      val testFailures = testExecutions.flatMap { it.second }

      log().info("Executed ${testExecutions.size} test scenarios with ${testFailures.size} failures")
      if (testFailures.isNotEmpty()) {
         val failureMessages = testFailures.joinToString("\n") { it.toString() }
         fail(failureMessages)
      }
   }

   private fun executeTestScenario(schemas: List<VersionedSchema>, testCase: VyneTestCase): List<VyneTestFailure> {
      log().info("Executing test ${testCase.test}")

      val (vyne, stubService) = replayingVyne(schemas, testCase)

      val queryResult = vyne.execute(testCase.scenario.query)

      val testFailures = testCase.scenario.response.results.map { (typeName, typeNamedInstance) ->
         val type = vyne.type(typeName)
         val typedInstance = when (typeNamedInstance.value) {
            is List<*> -> {
               // At this point, we have a top-level collection, which we previously
               // weren't able to deserialzie with an inner collection type, as they're not
               // returned (the typeName will be UnknownCollectionType)
               // Therefore, map the collection values to a TypedCollection
               // using the type we've just been given from the response payload.
               val collectionMembers = (typeNamedInstance.value as List<*>).map { TypedInstance.fromNamedType(it as TypeNamedInstance, vyne.schema) }
               TypedCollection(type, collectionMembers)
            }
            else -> TypedInstance.fromNamedType(typeNamedInstance, vyne.schema)
         }

         type to typedInstance
      }.mapNotNull { (originalResponseType, originalResponseValue) ->
         val testResultForType = queryResult[originalResponseType]
            ?: return@mapNotNull SimpleTestFailure("Test case ${testCase.test} failed because response type ${originalResponseType.name.name} was present in the original response, but not found in the test response")
         if (testResultForType != originalResponseValue) {
            NotEqualTestFailure("Test case ${testCase.test} failed because response type ${originalResponseType.name.name} did not equal the original value returned", expected = originalResponseValue, actual = testResultForType)
         } else {
            null
         }
      }
      return testFailures
   }
}

interface VyneTestFailure {
   val message: String
}

data class SimpleTestFailure(override val message: String) : VyneTestFailure
data class NotEqualTestFailure(override val message: String, val expected: TypedInstance, val actual: TypedInstance) : VyneTestFailure

data class VyneTestCase(
   val test: String,
   val scenario: QueryHistoryRecord
)

// Taken from the Vyne Query service, but don't wanna extract or couple,
// so duplicated
data class QueryHistoryRecord(
   val query: Query,
   val response: LightweightQueryResult,
   val timestamp: Instant = Instant.now()
) {
   val id: String = response.queryResponseId
}

data class VersionedSchema(val name: String, val version: String, val content: String)

// We use this since the QueryResult that we're capturing
// has been trimmed down during serialziation (since it's intended for the UI,
// and we don't want to send too much info)
// This is probabably enough for what we're trying to acheive
data class LightweightQueryResult(
   override val queryResponseId: String,
   override val isFullyResolved: Boolean,
   override val remoteCalls: List<RemoteCall>,
   override val timings: Map<OperationType, Long>,
   override val vyneCost: Long,
   override val resultMode: ResultMode = ResultMode.VERBOSE,
   val results: Map<String, TypeNamedInstance>

) : QueryResponse {
   override val profilerOperation: ProfilerOperation? = null
}


fun replayingVyne(schemas: List<VersionedSchema>, testCase: VyneTestCase): Pair<Vyne, ReplayingOperationInvoker> {
   val schema = schemas.joinToString("\n") { it.content }
   val taxiSchema = TaxiSchema.from(schema)
   val operationInvoker = ReplayingOperationInvoker(testCase.scenario.response.remoteCalls, taxiSchema)
   val queryEngineFactory = QueryEngineFactory.withOperationInvokers(operationInvoker)
   val vyne = Vyne(queryEngineFactory).addSchema(taxiSchema)
   return vyne to operationInvoker
}

