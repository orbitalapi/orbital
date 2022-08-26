package io.vyne.schema.publisher.http

import com.winterbe.expekt.should
import io.vyne.PackageMetadata
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.http.MockWebServerRule
import io.vyne.schema.publisher.*
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Rule
import org.junit.Test
import org.springframework.web.reactive.function.client.WebClient
import reactor.kotlin.test.test
import reactor.test.StepVerifier
import java.time.Duration
import java.util.concurrent.TimeUnit

class ExpiringSourcesStoreTest {
   @Rule
   @JvmField
   val server = MockWebServerRule()

   private val brokerOrderTaxi = VersionedSource(
      "com.broker", "1.0.0", """
         namespace broker {
           model OrderView {
              orderId: String
           }
         }
      """.trimIndent()
   )
   private val sourcePackageSubmission = KeepAlivePackageSubmission(
      SourcePackage(
         PackageMetadata.from("com.foo", "test"),
         listOf(brokerOrderTaxi)
      ),
      publisherConfiguration().keepAlive
   )

   @Test
   fun `registration should set last heartbeat`() {
      val httpKeepALiveStrategyMonitor = HttpPollKeepAliveStrategyMonitor(webClientBuilder = WebClient.builder())
      val store = ExpiringSourcesStore(keepAliveStrategyMonitors = listOf(httpKeepALiveStrategyMonitor))
      store.submitSources(sourcePackageSubmission)
      httpKeepALiveStrategyMonitor.lastPingTimes.size.should.equal(1)
   }

   @Test
   fun `schemas that failed to heartbeat are removed`() {
      StepVerifier.setDefaultTimeout(Duration.ofSeconds(30))
      val httpPollPeriodInSecsForRegistration = 2L
      server.get().apply {
         dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
               TimeUnit.SECONDS.sleep(httpPollPeriodInSecsForRegistration + 2L)
               return MockResponse()
            }
         }
      }

      val httpPollKeepAliveStrategyMonitor = HttpPollKeepAliveStrategyMonitor(
         webClientBuilder = WebClient.builder(),
         httpRequestTimeout = Duration.ofSeconds(httpPollPeriodInSecsForRegistration)
      )

      val store = ExpiringSourcesStore(keepAliveStrategyMonitors = listOf(httpPollKeepAliveStrategyMonitor))
      store.currentSources
         .test()
         .expectSubscription()
         .then { store.submitSources(sourcePackageSubmission) }
         .expectNextMatches { currentState ->
            currentState.currentPackages.should.have.size(1)
            currentState.removedSchemaIds.isEmpty()
            true
         }
         .expectNextMatches { currentState ->
            currentState.deltas.should.have.size(1)
            val health = currentState.deltas.single() as PublisherHealthUpdated
            health.health.status.should.equal(PublisherHealth.Status.Unhealthy)
            true
         }
         .thenCancel()
         .verify()
   }

   private fun publisherConfiguration(pollFrequencyInSeconds: Long = 15L): PublisherConfiguration {
      return PublisherConfiguration(
         "publisher1",
         HttpPollKeepAlive(
            pollFrequency = Duration.ofSeconds(pollFrequencyInSeconds),
            pollUrl = "http://localhost:${server.port}/ping"
         )
      )
   }
}
