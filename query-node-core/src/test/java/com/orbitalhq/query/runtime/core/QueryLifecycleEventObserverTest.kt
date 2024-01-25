package com.orbitalhq.query.runtime.core

import app.cash.turbine.test
import app.cash.turbine.testIn
import app.cash.turbine.withTurbineTimeout
import com.orbitalhq.expectTypedObject
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.json.parseJsonModel
import com.orbitalhq.query.QueryEvent
import com.orbitalhq.query.QueryEventConsumer
import com.orbitalhq.query.QueryResult
import com.orbitalhq.query.QuerySpecTypeNode
import com.orbitalhq.query.QueryStartEvent
import com.orbitalhq.query.StreamingQueryCancelledEvent
import com.orbitalhq.query.TaxiQlQueryResultEvent
import com.orbitalhq.testVyne
import com.winterbe.expekt.should
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mu.KotlinLogging
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import reactor.kotlin.test.test
import reactor.kotlin.test.verifyError
import reactor.test.StepVerifier
import java.util.concurrent.Executors
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {  }
class QueryLifecycleEventObserverTest {


   private val queryEventConsumer = object : QueryEventConsumer {
      private val messageSink = Sinks.many().unicast().onBackpressureBuffer<QueryEvent>()
      val messages: Flux<QueryEvent> = messageSink.asFlux()

      override fun handleEvent(event: QueryEvent) {
         messageSink.tryEmitNext(event)
      }

      override fun recordResult(operation: OperationResult, queryId: String) {
      }
   }

   private val taxiDef = """
          type ClientId inherits String
          type ClientName inherits String
          model Client {
            clientId : ClientId
            clientName: ClientName
         }

         service ClientService {
            operation streamClients():Stream<Client>
         }
   """.trimIndent()
   @OptIn(ExperimentalCoroutinesApi::class)
   @Test
   fun `When streaming query is cancelled StreamingQueryCancelledEvent event is published`() = runTest {
      val (vyne, stub) = testVyne(taxiDef)
      val typedInstance = TypedInstance.from(
         vyne.type("Client"),
         mapOf("clientId" to "123", "clientName" to "Marty"),
         vyne.schema
      )

      val flow = flow {
         while(true) {
            emit(typedInstance)
            delay(1000L)
         }
      }.flowOn(Dispatchers.IO)

      stub.addResponseFlow("streamClients") { _, _ -> flow }

      val queryResult = vyne.query(
         """stream { Client }
      """.trimMargin()
      )

     val queryResponse =  QueryLifecycleEventObserver(queryEventConsumer, null)
         .responseWithQueryHistoryListener("stream { Client }", queryResult)


      (queryResponse as QueryResult).results.test {
         expectTypedObject()
         StepVerifier.create(queryEventConsumer.messages)
            .recordWith { mutableListOf<QueryEvent>() }
            .thenConsumeWhile {
               logger.info { it.javaClass.name }
               if (it is TaxiQlQueryResultEvent) {
                  queryResult.requestCancel()
               }
               it !is StreamingQueryCancelledEvent
            }
            .expectRecordedMatches {
               it.filterIsInstance<StreamingQueryCancelledEvent>().size == 1
            }
            .expectNextMatches { it is StreamingQueryCancelledEvent }
            .thenCancel()
            .verify()
      }
   }
}
