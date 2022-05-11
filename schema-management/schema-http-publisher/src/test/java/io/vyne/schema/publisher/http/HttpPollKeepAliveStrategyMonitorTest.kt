package io.vyne.schema.publisher.http

import com.winterbe.expekt.should
import io.vyne.http.MockWebServerRule
import io.vyne.http.response
import io.vyne.models.json.RelaxedJsonMapper.jackson
import io.vyne.schema.publisher.HttpPollKeepAlive
import io.vyne.schema.publisher.PublisherConfiguration
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit


class HttpPollKeepAliveStrategyMonitorTest {
   @Rule
   @JvmField
   val server = MockWebServerRule()

   @Test
   fun `When a publisher responds keep alive on time it is kept as active`() {
      val invokedPaths = ConcurrentHashMap<String, Int>()
      server.prepareResponse(invokedPaths, "/ping" to response(jackson.writeValueAsString(mapOf("OK" to true))))

      val httpPollKeepAliveStrategyMonitor = HttpPollKeepAliveStrategyMonitor(webClientBuilder = WebClient.builder())
      val publisherWithHttpPoll = publisherConfig()
      httpPollKeepAliveStrategyMonitor.monitor(publisherWithHttpPoll)

      StepVerifier.create(httpPollKeepAliveStrategyMonitor.terminatedInstances)
         .expectSubscription()
         .expectNoEvent(Duration.ofSeconds(5L))
         .thenCancel()
         .verify()

      httpPollKeepAliveStrategyMonitor.lastPingTimes.size.should.equal(1)
      invokedPaths["/ping"].should.equal(1)
   }

   @Test
   fun `When a publisher responds with 204 then it is kept as active`() {
      server.prepareResponse { response -> response.setResponseCode(204) }
      val httpPollKeepAliveStrategyMonitor = HttpPollKeepAliveStrategyMonitor(webClientBuilder = WebClient.builder())
      val publisherWithHttpPoll = publisherConfig()
      httpPollKeepAliveStrategyMonitor.monitor(publisherWithHttpPoll)

      StepVerifier.create(httpPollKeepAliveStrategyMonitor.terminatedInstances)
         .expectSubscription()
         .expectNoEvent(Duration.ofSeconds(5L))
         .thenCancel()
         .verify()

      httpPollKeepAliveStrategyMonitor.lastPingTimes.size.should.equal(1)
   }

   @Test
   fun `When a publisher responds with 301 then it is reported as terminated`() {
      server.prepareResponse { response ->
         response
            .setResponseCode(301)
      }

      val httpPollKeepAliveStrategyMonitor = HttpPollKeepAliveStrategyMonitor(webClientBuilder = WebClient.builder())
      val publisherWithHttpPoll = publisherConfig()
      httpPollKeepAliveStrategyMonitor.monitor(publisherWithHttpPoll)

      StepVerifier.create(httpPollKeepAliveStrategyMonitor.terminatedInstances)
         .expectSubscription()
         .expectNext(publisherConfig())
         .thenCancel()
         .verify()

      httpPollKeepAliveStrategyMonitor.lastPingTimes.should.be.empty
   }

   @Test
   fun `When a ping request times out publisher is reported as dead`() {
      server.get().apply {
         dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
               TimeUnit.SECONDS.sleep(7L)
               return MockResponse()
            }
         }
      }

      val httpPollKeepAliveStrategyMonitor = HttpPollKeepAliveStrategyMonitor(
         webClientBuilder = WebClient.builder(),
         httpRequestTimeout = Duration.ofSeconds(5)
      )
      val publisherWithHttpPoll = publisherConfig()
      httpPollKeepAliveStrategyMonitor.monitor(publisherWithHttpPoll)

      StepVerifier.create(httpPollKeepAliveStrategyMonitor.terminatedInstances)
         .expectSubscription()
         .expectNext(publisherConfig())
         .thenCancel()
         .verify()

      httpPollKeepAliveStrategyMonitor.lastPingTimes.should.be.empty

   }

   private fun publisherConfig(keepAliveInSeconds: Long = 2L): PublisherConfiguration {
      return PublisherConfiguration(
         "testPublisher",
         HttpPollKeepAlive(
            pollFrequency = Duration.ofSeconds(keepAliveInSeconds),
            pollUrl = "http://localhost:${server.port}/ping"
         )
      )
   }
}
