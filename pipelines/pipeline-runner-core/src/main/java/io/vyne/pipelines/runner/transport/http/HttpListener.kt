package io.vyne.pipelines.runner.transport.http

import io.vyne.models.TypedInstance
import io.vyne.pipelines.MessageContentProvider
import io.vyne.pipelines.PipelineDirection
import io.vyne.pipelines.PipelineInputMessage
import io.vyne.pipelines.PipelineInputTransport
import io.vyne.pipelines.PipelineLogger
import io.vyne.pipelines.PipelineTransportSpec
import io.vyne.pipelines.runner.transport.PipelineInputTransportBuilder
import io.vyne.pipelines.runner.transport.PipelineTransportFactory
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * Rest controller which accepts requests at /api/triggers
 * and routes to a configured HttpListener as defined in a pipeline
 */
@RestController
class HttpListenerTriggerController {

   private val handlers = mutableMapOf<HttpListenerTransportSpec, HttpListenerInput>()

   fun registerHandler(spec: HttpListenerTransportSpec, handler: HttpListenerInput) {
      handlers[spec] = handler
   }

   @RequestMapping("/api/triggers/**")
   fun handleTriggerRequest(exchange: ServerWebExchange): Mono<Void> {
      val triggerPath = exchange.request.path.toString().removePrefix("/api/triggers")
      val matchingHandlers = handlers.keys.filter { it.path == triggerPath }
      return when (matchingHandlers.size) {
         0 -> {
            exchange.response.statusCode = HttpStatus.NOT_FOUND
            exchange.response.setComplete()
         }
         1 -> {
            val handlerSpec = matchingHandlers.first()
            val handler = handlers[handlerSpec]
               ?: error("Matched to spec ${handlerSpec.description}, but not handler was present.")
            logger.info { "Matched inbound trigger request to handler ${handlerSpec.description}" }
            val emitResult = handler.sink.tryEmitNext(exchange.request)
            if (emitResult.isFailure) {
               logger.warn { "Failed to emit inbound request on path $triggerPath as it was rejected by the handler - ${emitResult.name}" }
               exchange.response.statusCode = HttpStatus.INTERNAL_SERVER_ERROR
               exchange.response.setComplete()
            } else {
               exchange.response.statusCode = HttpStatus.OK
               exchange.response.setComplete()
            }
         }
         else -> {
            exchange.response.statusCode = HttpStatus.NOT_FOUND
            // Need to do something better here.
            logger.error { "Found ${matchingHandlers.size} matching handlers for trigger path $triggerPath.  Rejecting" }
            exchange.response.setComplete()
         }
      }
   }
}

@Component
class HttpListenerBuilder(private val controller: HttpListenerTriggerController) :
   PipelineInputTransportBuilder<HttpListenerTransportSpec> {
   override fun canBuild(spec: PipelineTransportSpec): Boolean {
      return spec.type == HttpListenerTransport.TYPE && spec.direction == PipelineDirection.INPUT
   }

   override fun build(
      spec: HttpListenerTransportSpec,
      logger: PipelineLogger,
      transportFactory: PipelineTransportFactory
   ): PipelineInputTransport {
      val listener = HttpListenerInput(
         spec, logger, transportFactory
      )
      controller.registerHandler(spec, listener)
      return listener
   }
}


class HttpListenerInput(
   private val spec: HttpListenerTransportSpec,
   private val logger: PipelineLogger,
   private val transportFactory: PipelineTransportFactory
) : PipelineInputTransport {
   val sink: Sinks.Many<ServerHttpRequest> = Sinks.many().unicast().onBackpressureError()
   private val flux = sink.asFlux()
      .map { request ->
         PipelineInputMessage(
            Instant.now(),
            emptyMap(),
            HttpRequestMessageContentProvider(request)
         )
      }
   override val feed: Flux<PipelineInputMessage> = flux
   override val description: String = "Http listener at path ${spec.path}"

   override fun type(schema: Schema): Type {
      return schema.type(spec.payloadType)
   }

}

private class HttpRequestMessageContentProvider(private val request: ServerHttpRequest) : MessageContentProvider {
   override fun asString(logger: PipelineLogger): String {
      val baos = ByteArrayOutputStream()
      request
         .body
         .publishOn(Schedulers.boundedElastic())
         .flatMap { body ->
            Mono.create<Boolean> { sink ->
               body.asInputStream().copyTo(baos)
               sink.success()
            }.publishOn(Schedulers.boundedElastic())
         }
         .publishOn(Schedulers.boundedElastic())
         .subscribeOn(Schedulers.boundedElastic())
         .blockLast()
      return baos.toString()
   }

   override fun writeToStream(logger: PipelineLogger, outputStream: OutputStream) {
      request.body.subscribe { buffer -> buffer.asInputStream().copyTo(outputStream) }
   }

   override fun readAsTypedInstance(logger: PipelineLogger, inputType: Type, schema: Schema): TypedInstance {
      return TypedInstance.from(inputType, asString(logger), schema)
   }

}
