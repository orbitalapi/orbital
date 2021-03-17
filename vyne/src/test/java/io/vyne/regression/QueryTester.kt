package io.vyne.regression

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.io.Resources
import io.vyne.VersionedSource
import io.vyne.Vyne
import io.vyne.VyneCacheConfiguration
import io.vyne.models.Provided
import io.vyne.models.TypeNamedInstance
import io.vyne.models.TypeNamedInstanceDeserializer
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.query.Query
import io.vyne.query.QueryEngineFactory
import io.vyne.query.VyneJacksonModule
import io.vyne.query.history.QueryHistoryRecord
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.log
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * This class replays the tests found in resources/scenarios
 * These are captured outputs from the Vyne UI, which we then replay to ensure
 * no regressions.
 *
 * Tests exist in a directory containing a schema.json, and then a bunch of test scneario,
 * also named xxxx.json
 *
 * To add a test:
 * Run Vyne and the services you want to test
 * Execute the scenario within the UI
 * Click on Download button in query result window (you can also use Query history menu to fetch the data for past queries)
 * Select 'Download as Test Spec'option
 * extract the 'zip' under resources/scenarios
 *
 * Bosh.
 *
 * Find the entry you wan
 */
class QueryTester {
   private val objectMapper: ObjectMapper = jacksonObjectMapper()
      .registerModule(VyneJacksonModule())
      .registerModule(JavaTimeModule())
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)


   private val queryHistoryRecordTypeRef: TypeReference<QueryHistoryRecord<out Any>> = object : TypeReference<QueryHistoryRecord<out Any>>() {}
   private val listOfTypeNamedInstanceTypeRef: TypeReference<List<TypeNamedInstance>> = object : TypeReference<List<TypeNamedInstance>>() {}
   private val schemaFileName = "schema.json"

   fun runTest(root: File): List<VyneTestFailure> {
      val failures = mutableListOf<VyneTestFailure>()
      objectMapper.addMixIn(TypeNamedInstance::class.java, TypeNamedInstanceMixIn::class.java)
      root.walkTopDown().forEach {
         val schemaFile = it.toPath().resolve(schemaFileName).toFile()
         if (it.isDirectory && schemaFile.exists()) {
            val schema = schemaFile.readText()
            failures.addAll(executeTestsInDirectory(schema, it))
         }
      }
      return failures.toList()
   }

   private fun executeTestsInDirectory(schemaJson: String, testDirectory: File): List<VyneTestFailure> {
      val schemas = objectMapper.readValue<List<VersionedSource>>(schemaJson)
      val testFiles = testDirectory.listFiles { file ->
         file.name != schemaFileName && file.extension == "json"
      } ?: throw IllegalArgumentException("There is no test file in ${testDirectory.absolutePath}")

      val testExecutions = testFiles.map { testFile ->
         val historyRecord = objectMapper.readValue(testFile, queryHistoryRecordTypeRef)
         val testCase = VyneTestCase(testDirectory.name, historyRecord)
         testCase to executeTestScenario(schemas, testCase)
      }

      val testFailures = testExecutions.flatMap { it.second }

      log().info("Executed ${testExecutions.size} test scenarios with ${testFailures.size} failures")
      return testFailures
   }

   private fun executeTestScenario(schemas: List<VersionedSource>, testCase: VyneTestCase): List<VyneTestFailure> {
      log().info("Executing test ${testCase.test}")
      val (vyne, _) = replayingVyne(schemas, testCase)
      val queryResult = when (testCase.scenario.query) {
         is Query -> {
            runBlocking {vyne.execute(testCase.scenario.query as Query)}
         }
         is String -> {
            runBlocking {vyne.query(testCase.scenario.query as String)}
         }
         else -> {
            throw UnsupportedOperationException("Unsupported Query Type!")
         }
      }

      val testFailures = testCase.scenario.response.results.map { (typeName, typeNamedInstance) ->
         val type = vyne.type(typeName)
         val typedInstance = when (typeNamedInstance) {
            is List<*> -> {
               // At this point, we have a top-level collection, which we previously
               // weren't able to deserialzie with an inner collection type, as they're not
               // returned (the typeName will be UnknownCollectionType)
               // Therefore, map the collection values to a TypedCollection
               // using the type we've just been given from the response payload.
               val listOfTypedNamedInstances = objectMapper.readValue(objectMapper.writeValueAsString(typeNamedInstance), listOfTypeNamedInstanceTypeRef)
               val collectionMembers = listOfTypedNamedInstances.map { TypedInstance.fromNamedType(it, vyne.schema, source = Provided) }
               TypedCollection(type, collectionMembers)
            }
            else -> TypedInstance.fromNamedType(typeNamedInstance as TypeNamedInstance, vyne.schema, source = Provided)
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
   val scenario: QueryHistoryRecord<out Any>
)

fun replayingVyne(schemas: List<VersionedSource>, testCase: VyneTestCase): Pair<Vyne, ReplayingOperationInvoker> {
   val taxiSchema = TaxiSchema.from(schemas)
   val operationInvoker = ReplayingOperationInvoker(testCase.scenario.response.remoteCalls, taxiSchema)
   val queryEngineFactory = QueryEngineFactory.withOperationInvokers(VyneCacheConfiguration.default(), operationInvoker)
   val vyne = Vyne(queryEngineFactory).addSchema(taxiSchema)
   return vyne to operationInvoker
}

// For Some reason TypeNamedInstanceDeserializer doesn't accept 'null' TypedNamedInstance values
// As I'm not quite sure the side effects of fixing that behaviour, I've modified this only for the 'regression tests'
class NullAwareTypeNamedInstanceDeserialiser : TypeNamedInstanceDeserializer() {
   override fun deserializeMap(rawMap: Map<Any, Any>): Any {
      val isTypeNamedInstance = rawMap.containsKey("typeName")
      if (isTypeNamedInstance) {
         val typeName = rawMap.getValue("typeName") as String
         val rawValue = rawMap["value"]
         val value = rawValue?.let { deserializeValue(it) }
         return TypeNamedInstance(typeName, value)
      }
      return rawMap.map { (key, value) ->
         key to deserializeValue(value)
      }.toMap()
   }

}

@JsonDeserialize(using = NullAwareTypeNamedInstanceDeserialiser::class)
class TypeNamedInstanceMixIn
