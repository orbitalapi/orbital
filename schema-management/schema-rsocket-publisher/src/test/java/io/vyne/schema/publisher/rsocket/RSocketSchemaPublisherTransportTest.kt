package io.vyne.schema.publisher.rsocket

import com.fasterxml.jackson.module.kotlin.readValue
import com.jayway.awaitility.Awaitility.await
import com.winterbe.expekt.should
import io.rsocket.ConnectionSetupPayload
import io.rsocket.Payload
import io.rsocket.RSocket
import io.rsocket.SocketAcceptor
import io.rsocket.core.RSocketServer
import io.rsocket.transport.netty.server.CloseableChannel
import io.rsocket.transport.netty.server.TcpServerTransport
import io.rsocket.util.DefaultPayload
import io.vyne.PackageMetadata
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.schema.api.SchemaSet
import io.vyne.schema.publisher.KeepAlivePackageSubmission
import io.vyne.schema.publisher.SchemaPublisherService
import io.vyne.schema.publisher.SourceSubmissionResponse
import io.vyne.schema.rsocket.CBORJackson
import io.vyne.schema.rsocket.TcpAddress
import io.vyne.utils.log
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.springframework.util.SocketUtils
import reactor.core.Disposable
import reactor.core.publisher.Mono
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


class RSocketSchemaPublisherTransportTest {
   private var server: CloseableChannel? = null
   private var publisherThread: Thread? = null

   @Before
   fun setup() {
      server = null
   }

   @After
   fun tearDown() {
      server?.dispose()
   }

   @Test
   @Ignore // for debugging
   fun `connect to real schema server`() {
      val publisher = createPublisher(7655, mutableListOf())
      Thread.sleep(30_000)
   }


   @Test
   fun `when server is running before client then client immediately publishes state on connection`() {
      val port = SocketUtils.findAvailableTcpPort()
      val collectedResponses = mutableListOf<SourceSubmissionResponse>()
      val connections = mutableListOf<RSocket>()
      startServer(port, response = SourceSubmissionResponse(emptyList(), SchemaSet.EMPTY), connections)
      // Wait for a bit before starting the server
      Thread.sleep(2000)

      createPublisher(port, collectedResponses)

      await().atMost(10, TimeUnit.SECONDS)
         .until<Boolean> { collectedResponses.size == 1 }
   }

   @Test
   fun `when client starts before server then client publishes state when connection eventually occurs`() {
      val port = SocketUtils.findAvailableTcpPort()
      val collectedResponses = mutableListOf<SourceSubmissionResponse>()
      createPublisher(port, collectedResponses)

      // Wait for a bit before starting the server
      Thread.sleep(2000)

      startServer(port, response = SourceSubmissionResponse(emptyList(), SchemaSet.EMPTY), mutableListOf())

      await().atMost(10, TimeUnit.SECONDS)
         .until<Boolean> { collectedResponses.size == 1 }
   }

   @Test
   fun `when schema changes before connection established then only the most recent schema is published`() {
      val port = SocketUtils.findAvailableTcpPort()
      val collectedResponses = mutableListOf<SourceSubmissionResponse>()
      val response = SourceSubmissionResponse(emptyList(), SchemaSet.EMPTY)
      val connections = mutableListOf<RSocket>()
      val collectedSubmissions = mutableListOf<SourcePackage>()

      val publisher = SchemaPublisherService(
         publisherId = "testPublisher",
         transport = RSocketSchemaPublisherTransport(TcpAddress("localhost", port))
      )

      // Publish the first schema
      publisher.publish(testPackage()).subscribe()

      // Now publish the second schema
      publisher.publish(
         VersionedSource.sourceOnly("type HelloWorld2").asPackage()
      ).subscribe()

      // Now start the schema server
      startServer(port, response, connections, collectedSubmissions)

      // Wait a bit to give enough time for everything to happen
      Thread.sleep(5000)

      // Ensure that only the second schema was published.
      collectedSubmissions.should.have.size(1)
      collectedSubmissions.single().sources.single().content.should.equal("type HelloWorld2")
   }

   @Test
   fun `republishes state after reconnecting after lost connection`() {
      val port = SocketUtils.findAvailableTcpPort()
      val collectedResponses = mutableListOf<SourceSubmissionResponse>()
      val publisher = createPublisher(port, collectedResponses)

      // Wait for a bit before starting the server
      Thread.sleep(2000)

      val response = SourceSubmissionResponse(emptyList(), SchemaSet.EMPTY)
      val connections = mutableListOf<RSocket>()
      startServer(port, response, connections)

      await().atMost(10, TimeUnit.SECONDS)
         .until<Boolean> { collectedResponses.size == 1 }

      stopServer(connections)

      Thread.sleep(2000)

      startServer(port, response, mutableListOf())

      await().atMost(10, TimeUnit.SECONDS)
         .until<Boolean> { collectedResponses.size == 2 }
   }

   @Test
   fun `can submit updates to schema`() {
      val port = SocketUtils.findAvailableTcpPort()

      val response = SourceSubmissionResponse(emptyList(), SchemaSet.EMPTY)
      val connections = mutableListOf<RSocket>()
      val collectedSubmissions = mutableListOf<SourcePackage>()
      val collectedResponses = mutableListOf<SourceSubmissionResponse>()

      startServer(port, response, connections, collectedSubmissions)

      val publisher = SchemaPublisherService(
         publisherId = "testPublisher",
         transport = RSocketSchemaPublisherTransport(TcpAddress("localhost", port))
      )

      publisher.responses
         .subscribe { submissionResponse ->
            collectedResponses.add(submissionResponse)
         }

      publisher.publish(testPackage()).subscribe()

      await().atMost(10, TimeUnit.SECONDS)
         .until<Boolean> { collectedResponses.size == 1 }

      // Submit an update
      publisher.publish(
         VersionedSource.sourceOnly("type HelloWorld2").asPackage()
      ).subscribe()

      await().atMost(10, TimeUnit.SECONDS)
         .until<Boolean> { collectedResponses.size == 2 }

      collectedSubmissions.should.have.size(2)
      collectedSubmissions.last().sources.single().content.should.equal("type HelloWorld2")
   }

   @Test
   fun `when reconnecting to server the most recent schema is published`() {

      // Setup
      val port = SocketUtils.findAvailableTcpPort()

      val response = SourceSubmissionResponse(emptyList(), SchemaSet.EMPTY)
      val connections = mutableListOf<RSocket>()
      val collectedSubmissions = mutableListOf<SourcePackage>()
      val collectedResponses = mutableListOf<SourceSubmissionResponse>()

      startServer(port, response, connections, collectedSubmissions)

      val publisher = SchemaPublisherService(
         publisherId = "testPublisher",
         transport = RSocketSchemaPublisherTransport(TcpAddress("localhost", port))
      )

      publisher.responses
         .subscribe { submissionResponse ->
            collectedResponses.add(submissionResponse)
         }

      // Publish first source
      publisher.publish(testPackage()).subscribe()

      await().atMost(5, TimeUnit.SECONDS)
         .until<Boolean> {
            collectedResponses.size == 1
         }

      // Submit an update
      publisher.publish(
         VersionedSource.sourceOnly("type HelloWorld2").asPackage()
      ).subscribe()

      Thread.sleep(2500)

      await().atMost(10, TimeUnit.SECONDS)
         .until<Boolean> { collectedResponses.size == 2 }


      // Kill the schema server
      stopServer(connections)

      // Wait a bit
      Thread.sleep(2000)

      startServer(port, response, connections, collectedSubmissions)

      // WE should now see a new submission
      await().atMost(10, TimeUnit.SECONDS)
         .until<Boolean> { collectedResponses.size == 3 }

      collectedSubmissions.should.have.size(3)
      collectedSubmissions.last().sources.single().content.should.equal("type HelloWorld2")

   }

   fun stopServer(connections: MutableList<RSocket>) {
      server!!.dispose()
      connections.forEach { connection ->
         connection.dispose()
      }
      connections.clear()
   }

   private fun VersionedSource.asPackage(
      organisation: String = "com.foo",
      name: String = "test",
      version: String = "1.0.0"
   ): SourcePackage {
      return SourcePackage(
         PackageMetadata.from(organisation, name, version),
         listOf(this)
      )
   }

   private fun testPackage() =
      VersionedSource.sourceOnly("type HelloWorld").asPackage()

   private fun createPublisher(
      port: Int,
      collectResponsesTo: MutableList<SourceSubmissionResponse>
   ): Thread {
      val thread = thread(start = true) {
         val publisher = SchemaPublisherService(
            publisherId = "testPublisher",
            transport = RSocketSchemaPublisherTransport(TcpAddress("localhost", port))
         )

         try {
            publisher.publish(testPackage())
               .subscribe { response -> collectResponsesTo.add(response) }
         } catch (e: Exception) {
            log().error("Failed to publish: ", e)
         }

      }
      return thread

   }

   /**
    * Starts an RSocket server on the provided port.
    * Use 0 to allow the OS to select a port, then
    * interrogate through server.address.port
    */
   fun startServer(
      port: Int = 0,
      response: SourceSubmissionResponse,
      connections: MutableList<RSocket>,
      collectedSubmissions: MutableList<SourcePackage> = mutableListOf()
   ): Disposable {
      log().info(
         """*********************************************
         |*********************************************
         |*** Starting RSocket Server on port $port ***
         |*********************************************
      """.trimMargin().trim()
      )

      val rsocketServer = RSocketServer
         .create(
            ConnectionWatchingResponseAcceptor(response, connections, collectedSubmissions)
         )
      return rsocketServer
         .bind(TcpServerTransport.create("localhost", port))
         .subscribe { closeable ->
            server = closeable
            closeable.onClose()
               .doFinally {
                  log().info("Server finally")
               }
               .subscribe {
                  log().info("Server closed")
               }
         }
   }

   internal class ConnectionWatchingResponseAcceptor(
      val response: Any,
      val connections: MutableList<RSocket>,
      val submissions: MutableList<SourcePackage> = mutableListOf()
   ) :
      SocketAcceptor {
      override fun accept(setupPayload: ConnectionSetupPayload, rSocket: RSocket): Mono<RSocket> {
         connections.add(rSocket)
         return Mono.just(object : RSocket {
            override fun requestResponse(payload: Payload?): Mono<Payload> {
               val receivedSubmission =
                  CBORJackson.defaultMapper
                     .readValue<KeepAlivePackageSubmission>(payload!!.data().array())

               submissions.add(receivedSubmission.sourcePackage)
               return Mono.just(
                  DefaultPayload.create(
                     CBORJackson.defaultMapper.writeValueAsBytes(response)
                  )
               )
            }
         })
      }

   }

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
