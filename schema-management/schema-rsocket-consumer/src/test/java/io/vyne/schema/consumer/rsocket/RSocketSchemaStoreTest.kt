package io.vyne.schema.consumer.rsocket

import com.jayway.awaitility.Awaitility
import io.rsocket.SocketAcceptor
import io.rsocket.core.RSocketServer
import io.rsocket.transport.netty.server.CloseableChannel
import io.rsocket.transport.netty.server.TcpServerTransport
import io.rsocket.util.DefaultPayload
import io.vyne.ParsedSource
import io.vyne.VersionedSource
import io.vyne.schema.api.SchemaSet
import io.vyne.schema.rsocket.CBORJackson
import io.vyne.schema.rsocket.SchemaServerRSocketFactory
import io.vyne.schema.rsocket.TcpAddress
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.utils.log
import org.junit.After
import org.junit.Ignore
import org.junit.Test
import org.springframework.util.SocketUtils
import reactor.core.Disposable
import reactor.core.publisher.Sinks
import reactor.core.publisher.toFlux
import java.util.concurrent.TimeUnit

class RSocketSchemaStoreTest {

   private var server: CloseableChannel? = null

   @After
   fun tearDown() {
      server?.dispose()
   }

   @Test
   @Ignore // uysed at dev time
   fun `consume from real schema server`() {
      val events = mutableListOf<SchemaSetChangedEvent>()
      RSocketSchemaStore(
         SchemaServerRSocketFactory(TcpAddress("localhost", 7655))
      ).schemaChanged.toFlux().subscribe { event -> events.add(event) }

      Awaitility.await().atMost(5, TimeUnit.SECONDS)
         .until<Boolean> { events.size == 1 }
   }

   @Test
   fun `consumes schema over rsocket`() {
      val port = SocketUtils.findAvailableTcpPort()
      val (sink, _) = startServer(port)

      val events = mutableListOf<SchemaSetChangedEvent>()
      RSocketSchemaStore(
         SchemaServerRSocketFactory(TcpAddress("localhost", port))
      ).schemaChanged.toFlux().subscribe { event -> events.add(event) }

      sink.tryEmitNext(SchemaSet.fromParsed(
         listOf(ParsedSource(VersionedSource.sourceOnly("type HelloWorld"))),
         1
      ))

      Awaitility.await().atMost(5, TimeUnit.SECONDS)
         .until<Boolean> { events.size == 1 }
   }

   @Test
   fun `duplicate events from server don't emit multiple events`() {
      val port = SocketUtils.findAvailableTcpPort()
      val (sink, _) = startServer(port)

      val events = mutableListOf<SchemaSetChangedEvent>()
      RSocketSchemaStore(
         SchemaServerRSocketFactory(TcpAddress("localhost", port))
      ).schemaChanged.toFlux().subscribe { event -> events.add(event) }

      val schemaSet = SchemaSet.fromParsed(
         listOf(ParsedSource(VersionedSource.sourceOnly("type HelloWorld"))),
         1
      )

      // Emit the same event multiple times
      sink.tryEmitNext(schemaSet)
      sink.tryEmitNext(schemaSet)
      sink.tryEmitNext(schemaSet)

      Awaitility.await().atMost(5, TimeUnit.SECONDS)
         .until<Boolean> { events.size == 1 }
   }


   fun startServer(
      port: Int = 0,
   ): Pair<Sinks.Many<SchemaSet>, Disposable> {
      val sink = Sinks.many().unicast().onBackpressureBuffer<SchemaSet>()
      val flux = sink.asFlux()

      log().info(
         """*********************************************
         |*********************************************
         |*** Starting RSocket Server on port $port ***
         |*********************************************
      """.trimMargin().trim()
      )

      val rsocketServer = RSocketServer
         .create(
            SocketAcceptor.forRequestStream { payload ->
               flux.map {
                  DefaultPayload.create(CBORJackson.defaultMapper.writeValueAsBytes(it))
               }
            }
         )
      return sink to rsocketServer
         .bind(TcpServerTransport.create("localhost", port))
         .subscribe { closeable ->
            server = closeable
         }
   }

}
