package io.vyne.queryService.lsp

import lang.taxi.lsp.TaxiLanguageServer
import lang.taxi.lsp.sourceService.WorkspaceSourceServiceFactory
import org.eclipse.lsp4j.jsonrpc.RemoteEndpoint
import org.eclipse.lsp4j.jsonrpc.json.JsonRpcMethod
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageServer
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

/**
 * An instance of a taxi Language Server that lives for as long as
 * as websocket session is alive.
 */
class WebsocketSessionLanguageServer(
   private val sourceServiceFactory: WorkspaceSourceServiceFactory
) {
   private val remoteEndpoint: RemoteEndpoint
   private val jsonHandler: MessageJsonHandler

   val languageServer = TaxiLanguageServer(
      workspaceSourceServiceFactory = sourceServiceFactory
   )
   private val messageSink = Sinks.many().unicast().onBackpressureBuffer<String>()
   val messages: Flux<String> = messageSink.asFlux()

   fun consume(message: String) {
      remoteEndpoint.consume(jsonHandler.parseMessage(message))
   }

   init {
      val supportedMethods: MutableMap<String, JsonRpcMethod> = LinkedHashMap()
      supportedMethods.putAll(ServiceEndpoints.getSupportedMethods(LanguageClient::class.java))
      supportedMethods.putAll(ServiceEndpoints.getSupportedMethods(LanguageServer::class.java))

      jsonHandler = MessageJsonHandler(supportedMethods)

      remoteEndpoint = RemoteEndpoint({ message ->
         val jsonMessage = jsonHandler.serialize(message)
         messageSink.tryEmitNext(jsonMessage)
      }, ServiceEndpoints.toEndpoint(languageServer))

      val proxy = ServiceEndpoints.toServiceObject(remoteEndpoint, LanguageClient::class.java)
      languageServer.connect(proxy)

      jsonHandler.methodProvider = remoteEndpoint
   }

   fun shutdown() {
      languageServer.shutdown()
   }
}
