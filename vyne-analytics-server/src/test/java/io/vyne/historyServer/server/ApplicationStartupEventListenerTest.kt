package io.vyne.historyServer.server

import io.vyne.historyServer.server.LineageRecordEventHandler.Companion.lineageRecord
import io.vyne.historyServer.server.QueryEndEventEventHandler.Companion.queryEndEvent
import io.vyne.historyServer.server.QuerySummaryTestClient.Companion.querySummary
import io.vyne.historyServer.server.QueryResultRowEventHandler.Companion.queryResultRow
import io.vyne.historyServer.server.RemoteCallResponseEventHandler.Companion.remoteCallResponse
import io.vyne.query.history.QuerySummary
import mu.KotlinLogging
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler
import org.springframework.test.context.junit4.SpringRunner
import reactor.core.Disposable
import reactor.test.StepVerifier
import java.time.Duration

private val logger = KotlinLogging.logger {}
@RunWith(SpringRunner::class)
@SpringBootTest(properties = [
   "spring.main.allow-bean-definition-overriding=true",
   "eureka.client.enabled=false",
   "vyne.schema.publicationMethod=LOCAL"])
class ApplicationStartupEventListenerTest {
   @Autowired
   lateinit var rsocketService: RSocketService

   @Autowired
   lateinit var rsocketStrategies: RSocketStrategies

   @Test
   fun `QuerySummary sent to history server`() {
      setupRSocketRequester(QuerySummaryTestClient(2))
      StepVerifier.create(rsocketService.queryCompletedMessages)
         .expectSubscription()
         .expectNextMatches { event ->
            (event is QuerySummary) && event.queryId == querySummary.queryId
         }
         .expectNextMatches { event -> (event is QuerySummary) && event.queryId == querySummary.queryId }
         .thenCancel()
         .verify(Duration.ofSeconds(10L))
   }

   @Test
   fun `QueryResultRow sent to history server`() {
      setupRSocketRequester(QueryResultRowEventHandler(2))
      StepVerifier.create(rsocketService.queryCompletedMessages)
         .expectSubscription()
         .expectNextMatches { event -> event == queryResultRow }
         .expectNextMatches { event -> event == queryResultRow }
         .thenCancel()
         .verify(Duration.ofSeconds(10L))

   }

   @Test
   fun `LineageRecord sent to history server`() {
      setupRSocketRequester(LineageRecordEventHandler())
      StepVerifier.create(rsocketService.queryCompletedMessages)
         .expectSubscription()
         .expectNextMatches { event -> event == lineageRecord }
         .thenCancel()
         .verify(Duration.ofSeconds(10L))
   }

   @Test
   fun `RemoteCallResponse sent to history server`() {
      setupRSocketRequester(RemoteCallResponseEventHandler())
      StepVerifier.create(rsocketService.queryCompletedMessages)
         .expectSubscription()
         .expectNextMatches { event -> event == remoteCallResponse }
         .thenCancel()
         .verify(Duration.ofSeconds(10L))
   }

   @Test
   fun `QueryEndEvent sent to history server`() {
      setupRSocketRequester(QueryEndEventEventHandler())
      StepVerifier.create(rsocketService.queryCompletedMessages)
         .expectSubscription()
         .expectNextMatches { event -> event == queryEndEvent }
         .thenCancel()
         .verify(Duration.ofSeconds(10L))
   }

   private fun setupRSocketRequester(candidateHandler: Any): Disposable {
      val responder =
         RSocketMessageHandler.responder(rsocketStrategies, candidateHandler)

      return RSocketRequester
         .builder()
         .dataMimeType(MediaType.APPLICATION_CBOR)
         .rsocketConnector { it.acceptor(responder) }.
         connectTcp("localhost", 7654)
         .subscribe()
   }
}
