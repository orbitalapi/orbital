package io.vyne.schemaStore

import io.vyne.VersionedSource
import io.vyne.httpSchemaPublisher.HttpPollKeepAliveStrategyMonitor
import io.vyne.schemaPublisherApi.HttpPollKeepAlive
import io.vyne.schemaPublisherApi.KeepAliveStrategyMonitor
import io.vyne.schemaPublisherApi.NoneKeepAliveStrategyMonitor
import io.vyne.schemaPublisherApi.PublisherConfiguration
import io.vyne.schemaPublisherApi.VersionedSourceSubmission
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Duration


@RunWith(SpringRunner::class)
@Import(TestConfig::class)
@EnableAutoConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [TaxiSchemaStoreService::class])
class TaxiSchemaStoreServiceSpringTest {
   @Autowired
   private val webClient: WebTestClient? = null

   @LocalServerPort
   private val port: Int? = null

   private val brokerOrderTaxi = VersionedSource("com.broker", "1.0.0", """
         namespace broker {
           model OrderView {
              orderId: String
           }
         }
      """.trimIndent())

   @Test
   fun `Can submit schemas with http keep alive strategy`() {
      val versionedSourceSubmission = VersionedSourceSubmission(listOf(brokerOrderTaxi),
         PublisherConfiguration("publisher1",
            HttpPollKeepAlive(pollFrequency = Duration.ofSeconds(60), pollUrl = "http://localhost:$port")))

      webClient!!.post()
         .uri("/api/schemas/taxi")
         .body(Mono.just(versionedSourceSubmission), VersionedSourceSubmission::class.java)
         .exchange()
         .expectStatus()
         .isOk
   }

   @Test
   fun `Can submit schemas with manual keep alive strategy`() {
      val versionedSourceSubmission = VersionedSourceSubmission(listOf(brokerOrderTaxi), PublisherConfiguration("publisher2"))
      webClient!!.post()
         .uri("/api/schemas/taxi")
         .body(Mono.just(versionedSourceSubmission), VersionedSourceSubmission::class.java)
         .exchange()
         .expectStatus()
         .isOk
   }
}

@TestConfiguration
class TestConfig {
   @Bean
   fun httpPollKeepAliveStrategyMonitor(): KeepAliveStrategyMonitor {
      return  HttpPollKeepAliveStrategyMonitor(webClientBuilder = WebClient.builder())
   }

   @Bean
   fun noneKeepAliveStrategyMonitor(): KeepAliveStrategyMonitor = NoneKeepAliveStrategyMonitor
}