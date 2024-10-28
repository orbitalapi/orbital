package com.orbitalhq.connectors.hazelcast

import com.hazelcast.config.Config
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.test.TestHazelcastInstanceFactory
import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.models.OperationResultReference
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.query.connectors.OperationInvocationParamMessage
import com.orbitalhq.query.graph.operationInvocation.cache.local.LocalCache
import com.orbitalhq.schema.api.SchemaSet
import com.orbitalhq.schema.consumer.SimpleSchemaStore
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.testVyne
import com.orbitalhq.utils.ManualClock
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.Instant

class HazelcastCacheProviderTest : DescribeSpec({
   describe("Hazelcast operation cache") {
      lateinit var hazelcast: HazelcastInstance

      lateinit var cacheProvider: HazelcastCachingInvokerProvider
      lateinit var clock: ManualClock

      val (vyne, stub) = testVyne(
         """
         closed model Person {
            id : PersonId inherits String
            name : FirstName inherits String
         }
         closed model Address {
            streetName : StreetName inherits String
         }
         service PersonService {
            operation findPerson(PersonId):Person
            operation findAddress(PersonId):Address
         }
      """.trimIndent()
      )
      val schemaStore = SimpleSchemaStore(SchemaSet.Companion.from(vyne.schema, 1))


      beforeTest {
         clock = ManualClock(Instant.now())
         hazelcast = TestHazelcastInstanceFactory()
            .newHazelcastInstance(Config().apply {
               serializationConfig = HazelcastBuilder.serializationConfig()
            })

         cacheProvider = HazelcastCachingInvokerProvider(
            hazelcast,
            schemaStore,
            10,
            "connectionName",
            "connectionAddress",
            clock,
            LocalCache.newLocalCache()
         )

         stub.clearAll()
      }
      afterTest {
         hazelcast.shutdown()
         stub.clearAll()
      }

      suspend fun invokeCache(cacheKey: String = "testKey"): Flux<TypedInstance> {
         val cache = cacheProvider.getHazelcastCachingInvoker(
            cacheKey, stub,
         )

         val service = vyne.getService("PersonService")
         val operation = service.operation("findPerson")
         val operationFlux = cache.invoke(
            OperationInvocationParamMessage(
               service,
               operation,
               parameters = listOf(
                  operation.parameters[0] to TypedInstance.from(
                     vyne.type("PersonId"),
                     "1",
                     vyne.schema
                  )
               ),
               mock { },
               "queryId",
               QueryOptions()
            )
         )
         return operationFlux
      }
      it("should cache the result") {
         stub.addResponse(
            "findPerson",
            vyne.parseJson("Person", """{ "id" : "1", "name" : "Jimmy" }"""),
            modifyDataSource = true
         )
         val result = invokeCache().collectList().block()!!
         result.shouldHaveSize(1)
         // First call comes from the stub, not the cache
         stub.calls["findPerson"].shouldHaveSize(1)
         // Data source should show the data came from a remote call
         result.single().source.shouldBeInstanceOf<OperationResultReference>()
            .operationName.fullyQualifiedName.shouldBe("PersonService@@findPerson")

         val resultFromCache = invokeCache().collectList().block()!!
         resultFromCache.shouldHaveSize(1)
         // Shouldn't have called the stub again
         stub.calls["findPerson"].shouldHaveSize(1)
         // Data source should show the data came from a remote call
         resultFromCache.single().source.shouldBeInstanceOf<CachedOperationResultReference>()
      }
      it("should cache if a second call comes when the first call is still inflight") {
         stub.addResponse("findPerson", vyne.parseJson("Person", """{ "id" : "1", "name" : "Jimmy" }"""))
         val operationFlux = invokeCache().collectList()
         // Send a second request before the first one is completed
         // (ie., before we've subscribed to the first one)
         val operationFlux2 = invokeCache().collectList()


         val result = operationFlux.block()!!
         result.shouldHaveSize(1)

         stub.calls["findPerson"].shouldHaveSize(1)


         val resultFromCache = operationFlux2.block()!!
         resultFromCache.shouldHaveSize(1)

         // Shouldn't have called the stub again
         stub.calls["findPerson"].shouldHaveSize(1)
      }

      it("expired items should not be returned from cache.") {
         // Item expires 10 seconds from now...
         val metadata = mapOf<String, Any>(TypedInstance.EXPIRY_METADATA to clock.instant().plusSeconds(10))
         stub.addResponse(
            "findPerson",
            vyne.parseJson(typeName = "Person", json = """{ "id" : "1", "name" : "Jimmy" }""", metadata = metadata)
         )

         val firstResult = invokeCache().collectList().block()!!
         firstResult.shouldHaveSize(1)
         // SHould've called the stub (the backing service), as it's the first call
         stub.calls["findPerson"].shouldHaveSize(1)


         // Move forward 1 minute, so item is now expired
         clock.advance(Duration.ofMinutes(1))
         val secondResult = invokeCache().collectList().block()!!

         secondResult.shouldHaveSize(1)
         // SHould've called the stub (the backing service), again, as the result has expired
         stub.calls["findPerson"].shouldHaveSize(2)
      }

      it("items with an expiry that has not expired should be served from cache") {
         val metadata = mapOf<String, Any>(TypedInstance.EXPIRY_METADATA to Instant.now().plusSeconds(3600))
         stub.addResponse(
            "findPerson",
            vyne.parseJson(typeName = "Person", json = """{ "id" : "1", "name" : "Jimmy" }""", metadata = metadata)
         )

         val firstResult = invokeCache().collectList().block()!!
         firstResult.shouldHaveSize(1)
         // SHould've called the stub (the backing service), as it's the first call
         stub.calls["findPerson"].shouldHaveSize(1)

         val secondResult = invokeCache().collectList().block()!!
         secondResult.shouldHaveSize(1)
         // Shouldn't have called again, as the item hasn't yet expired
         stub.calls["findPerson"].shouldHaveSize(1)
      }
      hazelcast.shutdown()
   }


})
