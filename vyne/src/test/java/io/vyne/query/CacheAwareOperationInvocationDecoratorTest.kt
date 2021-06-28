package io.vyne.query

import app.cash.turbine.test
import com.jayway.awaitility.Awaitility.await
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.TypedValue
import io.vyne.query.graph.operationInvocation.CacheAwareOperationInvocationDecorator
import io.vyne.query.graph.operationInvocation.OperationInvoker
import io.vyne.schemas.Operation
import io.vyne.schemas.Parameter
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Service
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import lang.taxi.types.PrimitiveType
import mu.KotlinLogging
import org.junit.Test
import reactor.core.publisher.Sinks
import reactor.kotlin.test.test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.test.assertFailsWith
import kotlin.time.ExperimentalTime

private val logger = KotlinLogging.logger {}

@ExperimentalTime
class CacheAwareOperationInvocationDecoratorTest {

   @Test
   fun testKeyGenerator() {
      val mockOperationInvoker = mock<OperationInvoker>()
      val mockQueryContext = mock<QueryContext>()
      val cacheAware = CacheAwareOperationInvocationDecorator(mockOperationInvoker)

      val type =
         Type(name = QualifiedName("type1"), sources = listOf(), taxiType = PrimitiveType.STRING, typeDoc = null)
      val mockedTypeInstance = mock<TypedInstance>()
      val service = Service(QualifiedName("srv1"), listOf(), listOf(), listOf(), listOf())
      val operation = Operation(
         qualifiedName = QualifiedName("op1@@op1"),
         returnType = type,
         parameters = listOf(),
         sources = listOf()
      )
      val params = listOf(
         element = Pair(
            first = Parameter(type),
            second = TypedInstance.from(type, null, mock(), source = Provided)
         )
      )
      runBlocking {
         whenever(mockOperationInvoker.invoke(any(), any(), any(), any(), any())).thenReturn(flow {

            emit(
               mockedTypeInstance
            )
         })
         cacheAware.invoke(service, operation, params, mockQueryContext, "MOCK_QUERY_ID").toList()
         cacheAware.invoke(service, operation, params, mockQueryContext, "MOCK_QUERY_ID").toList()
         verify(mockOperationInvoker, times(1)).invoke(
            service,
            operation,
            params,
            mockQueryContext,
            "MOCK_QUERY_ID"
         )
      }
   }

   val schema = TaxiSchema.from(
      """
         service Service {
            operation sayHello(input:String):String
            operation sayManyThings():String[]
         }
      """.trimIndent()
   )

   @Test
   fun `returns value from underlying invoker`(): Unit = runBlocking {
      val invoker = OnlyOnceStubInvoker { flowOf(TypedInstance.from(schema.type(PrimitiveType.STRING), "Hello", schema)) }
      val cachingInvoker = CacheAwareOperationInvocationDecorator(invoker)
      val (service, operation) = schema.operation("Service@@sayHello".fqn())
      val result = cachingInvoker.invoke(
         service,
         operation,
         listOf(param("A")),
         mock { }
      ).toList()
      result.should.have.size(1)
      result.first().value.should.equal("Hello")

      val cachedResult = cachingInvoker.invoke(
         service,
         operation,
         listOf(param("A")),
         mock { }
      ).toList()
      cachedResult.should.have.size(1)
      cachedResult.first().value.should.equal("Hello")
      // Should've only made it to the underlying invoker once
      invoker.invokedCalls.should.have.size(1)
   }

   @Test
   fun `throws exception from underlying invoker`(): Unit = runBlocking {
      val invoker = OnlyOnceStubInvoker { flow { error("Kaboom") } }
      val cachingInvoker = CacheAwareOperationInvocationDecorator(invoker)
      val (service, operation) = schema.operation("Service@@sayHello".fqn())
      assertFailsWith<Throwable>("Kaboom") {
         cachingInvoker.invoke(
            service,
            operation,
            listOf(param("A")),
            mock { }
         ).toList()
      }
   }

   private fun String.asTypedString():TypedInstance = TypedInstance.from(schema.type(PrimitiveType.STRING), this, schema)

   @Test
   fun `streams results without waiting for completion`():Unit = runBlocking {
      // Testing with flows is hard.
      // We're using a shared flow for this test, as it's the easiest way to emit into the flow
      // from a test.  However, the downside is that the flow can't complete.
      // Therefore, in this test we don't assert around completion, only around streaming consumption
      val flow = MutableSharedFlow<TypedInstance>(replay = 0)

      val invoker = OnlyOnceStubInvoker {   flow /*flowOf(TypedInstance.from(schema.type(PrimitiveType.STRING), "Hello", schema)) */ }
      val cachingInvoker = CacheAwareOperationInvocationDecorator(invoker)
      val (service, operation) = schema.operation("Service@@sayManyThings".fqn())

      // The first time, we emit while consuming to ensure that we
      // are getting streaming results, rather than a collected result set.
      cachingInvoker.invoke(
         service,
         operation,
         emptyList(),
         mock { }
      ).test {
         val words = listOf("Hello".asTypedString(), "World".asTypedString())
         flow.tryEmit(words[0])
         expect(words[0])
         flow.tryEmit(words[1])
         expect(words[1])
         cancelAndIgnoreRemainingEvents()
      }

      // Second time around, we shouldn't have to emit - should just
      // get the replayed values
      cachingInvoker.invoke(
         service,
         operation,
         emptyList(),
         mock { }
      ).test {
         expect("Hello".asTypedString())
         expect("World".asTypedString())
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun `when a request throws an exception the exception is rethrown on subsequent invocations`() {
      val invoker = OnlyOnceStubInvoker { inputs ->
         val (parameter, paramValue) = inputs.first()
         val input = paramValue.value as String
         if (input == "error") {
            flow { error("Kaboom") }
         } else {
            flowOf(TypedInstance.from(
               schema.type(PrimitiveType.STRING),
               "Hello",
               schema
            ))
         }
      }
      val inputs =
         listOf("A", "B", "error", "D", "E").map { param(it) }

      val results = invokeService(inputs, invoker)
      results.size.should.equal(25)
      results.filterIsInstance<Throwable>().should.have.size(5)
      await().atMost(1, TimeUnit.SECONDS).until {
         invoker.invokedCalls.size == 5
      }
   }

   @Test
   fun `sinks repeat exceptions`() {
      val sink = Sinks.many().replay().all<String>()
      sink.tryEmitNext("Hello")
      sink.tryEmitNext("World")
      sink.tryEmitError(NotImplementedError(""))

      val flux = sink.asFlux()

      flux.test()
         .expectNext("Hello")
         .expectNext("World")
         .expectError(NotImplementedError::class.java)
         .verify()

      // Subscribing again should emit the full value
      flux.test()
         .expectNext("Hello")
         .expectNext("World")
         .expectError(NotImplementedError::class.java)
         .verify()
   }

   @Test
   fun `multiple requests with the same key are processed sequentially`(): Unit = runBlocking {
      val invoker = OnlyOnceStubInvoker { inputs ->
         val (parameter, paramValue) = inputs.first()
         val input = paramValue.value as String
         flowOf(TypedInstance.from(
            schema.type(PrimitiveType.STRING),
            "Hello $input",
            schema
         ))
      }
      val inputs =
         listOf("A", "B", "C", "D", "E").map { param(it) }

      val results = invokeService(inputs, invoker)
      results.size.should.equal(25)
      listOf("A", "B", "C", "D", "E").map {  input ->
         results.count { it is TypedValue && it.value == "Hello $input" }.should.equal(5)
      }
      await().atMost(1, TimeUnit.SECONDS).until {
         invoker.invokedCalls.size == 5
      }
   }

   private fun invokeService(
      inputs: List<Pair<Parameter, TypedInstance>>,
      invoker: OnlyOnceStubInvoker
   ): List<Any> = runBlocking {

      val cachingInvoker = CacheAwareOperationInvocationDecorator(invoker)
      val (service, operation) = schema.operation("Service@@sayHello".fqn())

      val eventDispatcher: QueryContextEventDispatcher = mock { }
      val results = CopyOnWriteArrayList<Any>()
      val job = launch {
         inputs.flatMap { inputValue ->
            (0..4).map {
               launch {
                  logger.info { "Initiating call with input ${inputValue.second.toRawObject()}" }
                  val result = try {
                     cachingInvoker.invoke(service, operation, listOf(inputValue), eventDispatcher)
                        .toList().first()
                  } catch (exception: Exception) {
                     exception
                  }
                  results.add(result)
               }
            }
         }
      }
      job.join()
      await().atMost(1, TimeUnit.SECONDS).until { results.size == 25 }
      cachingInvoker.should.not.be.`null`
      results
   }

   private fun param(value: String): Pair<Parameter, TypedInstance> {
      val (service, operation) = schema.operation("Service@@sayHello".fqn())
      val instance = TypedInstance.from(schema.type(PrimitiveType.STRING), value, schema)
      return operation.parameters[0] to instance
   }

}

/**
 * Special stub invoker that throws an exception if there are multiple concurrent attempts
 * to invoke the same operation
 */
private class OnlyOnceStubInvoker(private val handler: (List<Pair<Parameter, TypedInstance>>) -> Flow<TypedInstance>) :
   OperationInvoker {
   private val callsInProgress = mutableMapOf<String, String>()
   val invokedCalls = mutableListOf<String>()
   override fun canSupport(service: Service, operation: RemoteOperation): Boolean = true
   override suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String?
   ): Flow<TypedInstance> {
      val cacheKey = CacheAwareOperationInvocationDecorator.generateCacheKey(
         service,
         operation,
         parameters
      )
      // Threadsafe check to ensure we're only invoking each operation
      // once at a time
      callsInProgress.compute(cacheKey) { key, value ->
         if (value != null) {
            error("Operation $key was invoked concurrently")
         }
         key
      }
      delay(500)
      callsInProgress.remove(cacheKey)
      invokedCalls.add(cacheKey)
      return handler.invoke(parameters)
   }

}
