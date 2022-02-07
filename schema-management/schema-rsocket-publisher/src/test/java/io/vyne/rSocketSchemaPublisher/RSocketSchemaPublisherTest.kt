package io.vyne.rSocketSchemaPublisher

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyVararg
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.vyne.schemaApi.SchemaSet
import io.vyne.schemaPublisherApi.PublisherConfiguration
import io.vyne.schemaPublisherApi.SourceSubmissionResponse
import io.vyne.schemeRSocketCommon.RSocketSchemaServerProxy
import org.junit.Test
import org.springframework.messaging.rsocket.RSocketRequester
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.test.StepVerifier
import java.time.Duration

class RSocketSchemaPublisherTest {
   private val mockRSocketSchemaServerProxy = mock<RSocketSchemaServerProxy>()
   @Test
   fun `when schema server connection is lost schema publisher triggers schemaServerConnectionLost event`() {
      var schemaSerDisconnectionSink: Sinks.Many<Unit>? = null
      val mockRSocketRequester = mock<RSocketRequester>()
      val mockRouteSpec = mock<RSocketRequester.RequestSpec>()
      val mockRetrieveSpec = mock<RSocketRequester.RetrieveSpec>()
      whenever(mockRSocketRequester.route(any(), anyVararg())).thenReturn(mockRouteSpec)
      whenever(mockRouteSpec.data(any())).thenReturn(mockRetrieveSpec)
      whenever(mockRetrieveSpec.retrieveMono(any<Class<SourceSubmissionResponse>>())).thenReturn(Mono.just(
         SourceSubmissionResponse(emptyList(), SchemaSet.EMPTY)
      ))

      whenever(mockRSocketSchemaServerProxy.schemaServerPublishSchemaConnection(any<PublisherConfiguration>(), any())).thenAnswer {
         schemaSerDisconnectionSink = it.getArgument<Sinks.Many<Unit>>(1)
         Mono.just(mockRSocketRequester)
      }

      val rsocketSchemaPublisher = RSocketSchemaPublisher(PublisherConfiguration("testId"), mockRSocketSchemaServerProxy, Duration.ofHours(1))

      rsocketSchemaPublisher.submitSchemas(emptyList(), emptyList())
      schemaSerDisconnectionSink!!.tryEmitNext(Unit)
      StepVerifier.create(Flux.from(rsocketSchemaPublisher.schemaServerConnectionLost))
         .expectSubscription()
         .expectNext(Unit)
         .thenCancel()
         .verify()

   }
}
