package com.orbitalhq.connectors.hazelcast

import com.hazelcast.test.TestHazelcastInstanceFactory
import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.connectors.OperationInvocationParamMessage
import com.orbitalhq.schema.api.SchemaSet
import com.orbitalhq.schema.consumer.SimpleSchemaStore
import com.orbitalhq.testVyne
import io.kotest.core.spec.style.DescribeSpec
import com.orbitalhq.models.json.parseJson
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldHaveSize

class HazelcastCacheProviderTest : DescribeSpec({
   describe("Hazelcast operation cache") {
      val hazelcast = TestHazelcastInstanceFactory(1).newHazelcastInstance()
      val (vyne,stub) = testVyne("""
         model Person {
            id : PersonId inherits String
            name : FirstName inherits String
         }
         model Address {
            streetName : StreetName inherits String
         }
         service PersonService {
            operation findPerson(PersonId):Person
            operation findAddress(PersonId):Address
         }
      """.trimIndent())
      val schemaStore = SimpleSchemaStore(SchemaSet.Companion.from(vyne.schema, 1))
      stub.addResponse("findPerson", vyne.parseJson("Person", """{ "id" : "1", "name" : "Jimmy" }"""))

      it("should cache the result") {
         val cacheProvider = HazelcastCacheProvider(
            hazelcast,
            schemaStore
         )
         val cacheKey = "testKey"
         val cache = cacheProvider.getCachingInvoker(
            cacheKey, stub
         )
         val service = vyne.getService("PersonService")
         val operation = service.operation("findPerson")
         val operationFlux = cache.invoke(
            OperationInvocationParamMessage(
               service,
               operation,
               parameters = listOf(operation.parameters[0] to TypedInstance.from(vyne.type("PersonId"), "1", vyne.schema)),
               mock {  },
               "queryId"
            )
         )
         val result = operationFlux.collectList().block()!!
         result.shouldHaveSize(1)

         stub.invocations.shouldHaveSize(1)

         // Now call it again
         val operationFlux2 = cache.invoke(
            OperationInvocationParamMessage(
               service,
               operation,
               parameters = listOf(operation.parameters[0] to TypedInstance.from(vyne.type("PersonId"), "1", vyne.schema)),
               mock {  },
               "queryId"
            )
         )
         val resultFromCache = operationFlux2.collectList().block()!!
         resultFromCache.shouldHaveSize(1)

         // Shouldn't have called the stub again
         stub.invocations.shouldHaveSize(1)
      }
      it("should cache if a second call comes when the first call is still inflight") {
         val cacheProvider = HazelcastCacheProvider(
            hazelcast,
            schemaStore
         )
         val cacheKey = "testKey"
         val cache = cacheProvider.getCachingInvoker(
            cacheKey, stub
         )
         val service = vyne.getService("PersonService")
         val operation = service.operation("findPerson")
         val operationFlux = cache.invoke(
            OperationInvocationParamMessage(
               service,
               operation,
               parameters = listOf(operation.parameters[0] to TypedInstance.from(vyne.type("PersonId"), "1", vyne.schema)),
               mock {  },
               "queryId"
            )
         )
         // Send a second request before the first one is completed
         // (ie., before we've subscribed to the first one)
         val operationFlux2 = cache.invoke(
            OperationInvocationParamMessage(
               service,
               operation,
               parameters = listOf(operation.parameters[0] to TypedInstance.from(vyne.type("PersonId"), "1", vyne.schema)),
               mock {  },
               "queryId"
            )
         )


         val result = operationFlux.collectList().block()!!
         result.shouldHaveSize(1)

         stub.invocations.shouldHaveSize(1)


         val resultFromCache = operationFlux2.collectList().block()!!
         resultFromCache.shouldHaveSize(1)

         // Shouldn't have called the stub again
         stub.invocations.shouldHaveSize(1)
      }
   }

})
