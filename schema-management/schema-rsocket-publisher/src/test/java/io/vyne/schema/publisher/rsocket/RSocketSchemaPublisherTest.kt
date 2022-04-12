package io.vyne.schema.publisher.rsocket

import com.nhaarman.mockito_kotlin.mock
import io.rsocket.ConnectionSetupPayload
import io.rsocket.Payload
import io.rsocket.RSocket
import io.rsocket.SocketAcceptor
import io.rsocket.core.RSocketServer
import io.rsocket.transport.netty.server.TcpServerTransport
import io.rsocket.util.DefaultPayload
import io.vyne.VersionedSource
import io.vyne.schema.rsocket.RSocketSchemaServerProxy
import io.vyne.schema.rsocket.SchemaUpdatesRSocketFactory
import mu.KotlinLogging
import org.junit.Test
import org.springframework.util.SocketUtils
import reactor.core.Disposable
import reactor.core.publisher.Mono
import java.time.Duration
import kotlin.concurrent.thread


class RSocketSchemaPublisherTest {
   private val mockRSocketSchemaServerProxy = mock<RSocketSchemaServerProxy>()

   private val logger = KotlinLogging.logger {}

   @Test
   fun `start and connect`() {
//      val legacy = Legacy().schemaServerPublishSchemaConnection(
//         PublisherConfiguration("test", RSocketKeepAlive),
//         jacksonObjectMapper()
//      )
//      val f = legacy.flatMap { requestor ->
//         requestor.route(RSocketRoutes.SCHEMA_SUBMISSION)
//            .data(
//               VersionedSourceSubmission(
//                  listOf(VersionedSource.sourceOnly("type Jimmy")),
//                  PublisherConfiguration("test", RSocketKeepAlive)
//               )
//            )
//            .retrieveMono(SourceSubmissionResponse::class.java)
//      }.block()!!
      val port = SocketUtils.findAvailableTcpPort()
//      val server = startServer(port)
//      Thread.sleep(2000)
      val publisher = createPublisher(7655)
      publisher.submitSchemas(testSources())
      Thread.sleep(10000)
   }

   @Test
   fun `when establishes connection then publishes current schema state`() {
      val port = SocketUtils.findAvailableTcpPort()
      val publisher = createPublisher(port)

      thread(start = true) {
         publisher.submitSchemasAsync(
            testSources()
         ).block(Duration.ofSeconds(100))
      }

      // Wait for a bit before starting the server
      Thread.sleep(2000)

      val server = startServer(port)

      Thread.sleep(10000)
   }

   private fun testSources() = listOf(
      VersionedSource.sourceOnly("type HelloWorld")
   )

   private fun createPublisher(port: Int) =
      RSocketSchemaPublisher("test", SchemaUpdatesRSocketFactory("ws://localhost:$port"))

   /**
    * Starts an RSocket server on the provided port.
    * Use 0 to allow the OS to select a port, then
    * interrogate through server.address.port
    */
   fun startServer(port: Int = 0): Disposable {
      return RSocketServer.create(
         SocketAcceptor.forRequestResponse({ payload ->
            TODO()
         })
      )
         .bind(TcpServerTransport.create("localhost", port))
         .subscribe()
//      val server = NettyRSocketServerFactory().apply {
//         this.setPort(port)
//      }
//         .create(EchoRequestResponseAcceptor())
//      server.start()
//      logger.info { "Started listening RSocket server on port ${server.address().port}" }
//      return server
   }

   @Test
   fun `if connection is lost then attempts to reconnect`() {

   }

   @Test
   fun `republishes state after reconnecting after lost connection`() {

   }

//   @Test
//   fun `when schema server connection is lost schema publisher triggers schemaServerConnectionLost event`() {
//      var schemaSerDisconnectionSink: Sinks.Many<Unit>? = null
//      val mockRSocketRequester = mock<RSocketRequester>()
//      val mockRouteSpec = mock<RSocketRequester.RequestSpec>()
//      val mockRetrieveSpec = mock<RSocketRequester.RetrieveSpec>()
//      whenever(mockRSocketRequester.route(any(), anyVararg())).thenReturn(mockRouteSpec)
//      whenever(mockRouteSpec.data(any())).thenReturn(mockRetrieveSpec)
//      whenever(mockRetrieveSpec.retrieveMono(any<Class<SourceSubmissionResponse>>())).thenReturn(
//         Mono.just(
//            SourceSubmissionResponse(emptyList(), SchemaSet.EMPTY)
//         )
//      )
//
//      whenever(
//         mockRSocketSchemaServerProxy.schemaServerPublishSchemaConnection(
//            any<PublisherConfiguration>(),
//            any()
//         )
//      ).thenAnswer {
//         schemaSerDisconnectionSink = it.getArgument<Sinks.Many<Unit>>(1)
//         Mono.just(mockRSocketRequester)
//      }
//
//      val rsocketSchemaPublisher =
//         RSocketSchemaPublisher(PublisherConfiguration("testId"), mockRSocketSchemaServerProxy, Duration.ofHours(1))
//
//      rsocketSchemaPublisher.submitSchemas(emptyList(), emptyList())
//      schemaSerDisconnectionSink!!.tryEmitNext(Unit)
//      StepVerifier.create(Flux.from(rsocketSchemaPublisher.schemaServerConnectionLost))
//         .expectSubscription()
//         .expectNext(Unit)
//         .thenCancel()
//         .verify()
//
//   }

   internal class EchoRequestResponseAcceptor : SocketAcceptor {
      override fun accept(setupPayload: ConnectionSetupPayload, rSocket: RSocket): Mono<RSocket> {
         return Mono.just(object : RSocket {
            override fun requestResponse(payload: Payload?): Mono<Payload> {
               return Mono.just(DefaultPayload.create(payload))
            }
         })
      }
   }
}
