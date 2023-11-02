package com.orbitalhq.cockpit.core.lsp

import com.orbitalhq.cockpit.core.lsp.querying.QueryCodeCompletionProvider
import com.orbitalhq.schemas.Schema
import lang.taxi.CompilerConfig
import lang.taxi.lsp.LspServicesConfig
import lang.taxi.lsp.TaxiCompilerService
import lang.taxi.lsp.TaxiLanguageServer
import lang.taxi.lsp.TaxiTextDocumentService
import lang.taxi.lsp.completion.CompositeCompletionService
import lang.taxi.lsp.sourceService.WorkspaceSourceServiceFactory
import lang.taxi.packages.utils.log
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
   private val sourceServiceFactory: WorkspaceSourceServiceFactory,
   schema: Schema
) {
   private val remoteEndpoint: RemoteEndpoint
   private val jsonHandler: MessageJsonHandler

   val languageServer = buildLanguageServer(schema)

   private fun buildLanguageServer(schema: Schema): TaxiLanguageServer {
      val compilerConfig = CompilerConfig()
      val compilerService = TaxiCompilerService(compilerConfig)
      val textDocumentService = TaxiTextDocumentService(
         LspServicesConfig(
            compilerService,
            completionService = CompositeCompletionService(
               listOf(
                  // Disabling the Editor completion service, as users are really only writing queries through our
                  // browser editor.  Can uncomment if useful.
//                  EditorCompletionService(compilerService.typeProvider),
                  QueryCodeCompletionProvider(compilerService.typeProvider, schema)
               )
            )
         )
      )
      return TaxiLanguageServer(
         compilerConfig,
         textDocumentService = textDocumentService,
         workspaceSourceServiceFactory = sourceServiceFactory
      )
   }

   private val messageSink = Sinks.many().unicast().onBackpressureBuffer<String>()
   val messages: Flux<String> = messageSink.asFlux()

   fun consume(message: String) {
      try {
         remoteEndpoint.consume(jsonHandler.parseMessage(message))
      } catch (e:NullPointerException) {
         if (e.message == """Cannot invoke "java.util.concurrent.CompletableFuture.thenAccept(java.util.function.Consumer)" because "future" is null""") {
            // Do nothing. We see this error whenever a user closes a connection, and
            // can't seem to find a way to prevent it.
         }
      }

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
