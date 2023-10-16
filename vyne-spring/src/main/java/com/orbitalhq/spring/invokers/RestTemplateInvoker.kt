package com.orbitalhq.spring.invokers

import io.netty.channel.ChannelOption
import com.orbitalhq.http.HttpHeaders.STREAM_ESTIMATED_RECORD_COUNT
import com.orbitalhq.http.UriVariableProvider
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.HttpExchange
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.*
import com.orbitalhq.spring.hasHttpMetadata
import com.orbitalhq.spring.http.DefaultRequestFactory
import com.orbitalhq.spring.http.HttpRequestFactory
import com.orbitalhq.spring.http.auth.schemes.AuthWebClientCustomizer
import com.orbitalhq.spring.http.auth.schemes.addAuthTokenAttributes
import com.orbitalhq.spring.query.formats.FormatSpecRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.reactive.asFlow
import lang.taxi.annotations.HttpService
import mu.KotlinLogging
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.*
import org.springframework.web.util.DefaultUriBuilderFactory
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

class RestTemplateInvoker(
   val schemaProvider: SchemaProvider,
   val webClient: WebClient,
   private val requestFactory: HttpRequestFactory = DefaultRequestFactory(),
   val formats: FormatSpecRegistry = FormatSpecRegistry.default(),
) : OperationInvoker {
   private val logger = KotlinLogging.logger {}

   constructor(
      schemaProvider: SchemaProvider,
      webClientBuilder: WebClient.Builder,
      authRequestCustomizer: AuthWebClientCustomizer,
      requestFactory: HttpRequestFactory = DefaultRequestFactory()
   )
      : this(
      schemaProvider,
      webClientBuilder
         .exchangeStrategies(
            ExchangeStrategies.builder().codecs { it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024) }.build()
         )
         .clientConnector(
            ReactorClientHttpConnector(
               HttpClient.create(

                  ConnectionProvider.builder("RestTemplateInvoker-Connection-Pool")
                     .maxConnections(500)
                     .maxIdleTime(Duration.ofMillis(10000.toLong()))
                     .maxLifeTime(Duration.ofMinutes(1.toLong()))
                     .metrics(true)
                     .fifo()
                     .pendingAcquireTimeout(Duration.ofMillis(20.toLong()))
                     .build()
               )
                  .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 100)
                  .keepAlive(true)
                  .compress(true) // support Gzipped responses
            )
         )
         .filter(authRequestCustomizer.authFromServiceNameAttribute)
         .build(),
      requestFactory
   )

   private val uriVariableProvider = UriVariableProvider()
   private val defaultUriBuilderFactory = DefaultUriBuilderFactory()

   init {
      logger.info { "Rest template invoker started" }
   }

   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return operation.hasHttpMetadata()
   }

   override suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String
   ): Flow<TypedInstance> {
      logger.info { "Invoking Operation ${operation.name} with parameters: ${parameters.joinToString(",") { (_, typedInstance) -> typedInstance.type.fullyQualifiedName + " -> " + typedInstance.toRawObject() }}" }

      val (_, url, method) = operation.httpOperationMetadata()
      val httpMethod = HttpMethod.valueOf(method)
      //val httpResult = profilerOperation.startChild(this, "Invoke HTTP Operation", OperationType.REMOTE_CALL) { httpInvokeOperation ->

      val absoluteUrl = prependServiceBaseUrl(service, url)
      val uriVariables = uriVariableProvider.getUriVariables(parameters, absoluteUrl)

      logger.info { "Operation ${operation.name} resolves to $absoluteUrl" }
      val typeInstanceParameters = parameters.map { it.second }
      val httpEntity = requestFactory.buildRequestBody(operation, typeInstanceParameters)
      val queryParams = requestFactory.buildRequestQueryParams(operation)

      val expandedUri = defaultUriBuilderFactory.expand(absoluteUrl, uriVariables)

      val contentType = getContentTypeFromResponseType(operation.returnType)

      //TODO - On upgrade to Spring boot 2.4.X replace usage of exchange with exchangeToFlow LENS-473
      val request = webClient
         .method(httpMethod)
         .uri { _ ->
            val uriBuilder = UriComponentsBuilder
               .fromUriString(absoluteUrl)
            (queryParams?.let { uriBuilder.queryParams(it) } ?: uriBuilder).build(uriVariables)
         }
         .contentType(contentType)
         .headers { consumer ->
            consumer.addAll(httpEntity.headers)
         }
         .addAuthTokenAttributes(service.name.fullyQualifiedName)
      if (httpEntity.hasBody()) {
         request.bodyValue(httpEntity.body)
      }

      logger.info { "[$queryId] - Performing $httpMethod to ${expandedUri.toASCIIString()}" }

      val remoteCallId = UUID.randomUUID().toString()
      val results = request
         .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)
         .exchange()
         .onErrorMap { error ->
            val remoteCall = RemoteCall(
               remoteCallId = remoteCallId,
               responseId = UUID.randomUUID().toString(),
               service = service.name,
               address = expandedUri.toASCIIString(),
               operation = operation.name,
               responseTypeName = operation.returnType.name,
               method = httpMethod.name(),
               requestBody = httpEntity.body,
               resultCode = -1,
               durationMs = 0,
               response = null,
               timestamp = Instant.now(),
               responseMessageType = ResponseMessageType.FULL,
               isFailed = true,
               exchange = HttpExchange(
                  url = expandedUri.toASCIIString(),
                  verb = httpMethod.name(),
                  requestBody = httpEntity.body?.toString(),
                  responseCode = -1,
                  responseSize = 0
               )
            )
            eventDispatcher.reportRemoteOperationInvoked(OperationResult.from(parameters, remoteCall), queryId)
            OperationInvocationException(
               "Failed to invoke service ${operation.name} at url $absoluteUrl - ${error.message ?: "No message in instance of ${error::class.simpleName}"}",
               0,
               remoteCall,
               parameters
            )
         }
         .metrics()
         .elapsed()
         .publishOn(Schedulers.boundedElastic())
         .flatMapMany { durationAndResponse ->
            val duration = durationAndResponse.t1
            val initiationTime = Instant.now().minusMillis(duration)
            val clientResponse = durationAndResponse.t2
            val isEventStream = clientResponse.headers().contentType().orElse(MediaType.APPLICATION_JSON)
               .isCompatibleWith(MediaType.TEXT_EVENT_STREAM)
            val responseMessageType = if (isEventStream) ResponseMessageType.EVENT else ResponseMessageType.FULL

            logger.info {
               "[$queryId] - $httpMethod to ${expandedUri.toASCIIString()} returned status ${clientResponse.statusCode()} after ${duration}ms"
            }

            fun remoteCall(responseBody: String, failed: Boolean = false): RemoteCall {
               return RemoteCall(
                  remoteCallId = remoteCallId,
                  responseId = UUID.randomUUID().toString(),
                  service = service.name,
                  address = expandedUri.toASCIIString(),
                  operation = operation.name,
                  responseTypeName = operation.returnType.name,
                  method = httpMethod.name(),
                  requestBody = httpEntity.body,
                  resultCode = clientResponse.statusCode().value(),
                  durationMs = duration,
                  response = responseBody,
                  timestamp = initiationTime,
                  responseMessageType = responseMessageType,
                  isFailed = failed,
                  exchange = HttpExchange(
                     url = expandedUri.toASCIIString(),
                     verb = httpMethod.name(),
                     requestBody = httpEntity.body?.toString(),
                     responseCode = clientResponse.statusCode().value(),
                     // Strictly, this isn't the size in bytes,
                     // but it's close enough until someone complains.
                     responseSize = responseBody.length,
                  )
               )
            }

            if (clientResponse.statusCode().isError) {
               return@flatMapMany clientResponse.bodyToMono<String>()
                  .switchIfEmpty(Mono.just(""))
                  .map { responseBody ->
                     val remoteCall = remoteCall(responseBody = responseBody, failed = true)
                     eventDispatcher.reportRemoteOperationInvoked(OperationResult.from(parameters, remoteCall), queryId)
                     throw OperationInvocationException(
                        "http error ${clientResponse.statusCode()} from url $expandedUri - $responseBody",
                        clientResponse.statusCode().value(),
                        remoteCall,
                        parameters
                     )
                  }
            }

            reportEstimatedResults(eventDispatcher, operation, clientResponse.headers())

            var firstResultReceived = false
            val count = AtomicInteger(0)
            if (isEventStream) {
               logger.debug { "Request to ${expandedUri.toASCIIString()} is streaming" }
               clientResponse.bodyToFlux<String>()
                  .flatMap { responseString ->
                     val remoteCall = remoteCall(responseBody = responseString)
                     handleSuccessfulHttpResponse(
                        responseString,
                        operation,
                        parameters,
                        remoteCall,
                        clientResponse.headers(),
                        eventDispatcher,
                        queryId
                     )
                  }
            } else {
               logger.debug { "Request to ${expandedUri.toASCIIString()} is not streaming" }
               if (!firstResultReceived) {
                  logger.info { "Received body of non-streaming response from ${expandedUri.toASCIIString()}" }
                  firstResultReceived = true
               }
               clientResponse.bodyToMono(String::class.java)
                  .flatMapMany { responseString ->
                     val remoteCall = remoteCall(responseBody = responseString)
                     handleSuccessfulHttpResponse(
                        responseString,
                        operation,
                        parameters,
                        remoteCall,
                        clientResponse.headers(),
                        eventDispatcher,
                        queryId
                     )
                  }
            }
         }

      return results.asFlow().flowOn(Dispatchers.IO)

   }

   private fun getContentTypeFromResponseType(returnType: Type): MediaType {
      val formatSpec = formats.forType(returnType)
      val mediaType = if (formatSpec != null) {
         try {
            MediaType.parseMediaType(formatSpec.mediaType)
         } catch (e: Exception) {
            logger.error { "Format spec ${formatSpec::class.simpleName} declares a media type of $${formatSpec.mediaType} which cannot be parsed to a standard MediaType" }
            MediaType.APPLICATION_JSON
         }

      } else {
         MediaType.APPLICATION_JSON // default to JSON
      }
      return mediaType
   }

   private fun prependServiceBaseUrl(service: Service, url: String): String {
      val serviceMetadata = service.metadata.singleOrNull { it.name == HttpService.NAME.fqn() }
         ?.let { metadata -> HttpService.fromParams(metadata.params) }

      return serviceMetadata?.let { it.baseUrl.removeSuffix("/") + "/" + url.removePrefix("/") }
         ?: url

   }

   private fun reportEstimatedResults(
      eventDispatcher: QueryContextEventDispatcher,
      operation: RemoteOperation,
      headers: ClientResponse.Headers
   ) {
      if (headers.header(STREAM_ESTIMATED_RECORD_COUNT).isNotEmpty()) {
         eventDispatcher.reportIncrementalEstimatedRecordCount(
            operation,
            Integer.valueOf(headers.header(STREAM_ESTIMATED_RECORD_COUNT)[0])
         )
      }
   }

   private fun handleSuccessfulHttpResponse(
      result: String,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      remoteCall: RemoteCall,
      headers: ClientResponse.Headers,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String
   ): Flux<TypedInstance> {
      // Logging responses in our logs is a security issue.  Let's not do this.
//      logger.debug { "Result of ${operation.name} was $result" }

      val isPreparsed = headers
         .header(com.orbitalhq.http.HttpHeaders.CONTENT_PREPARSED).let { headerValues ->
            headerValues != null && headerValues.isNotEmpty() && headerValues.first() == true.toString()
         }
      // If the content has been pre-parsed upstream, we don't evaluate accessors
      val evaluateAccessors = !isPreparsed
      val operationResult = OperationResult.from(parameters, remoteCall)

      val type = inferContentType(operation, headers, result)
      eventDispatcher.reportRemoteOperationInvoked(operationResult, queryId)
      val typedInstance = TypedInstance.from(
         type,
         result,
         schemaProvider.schema,
         source = operationResult.asOperationReferenceDataSource(),
         evaluateAccessors = evaluateAccessors,
         formatSpecs = formats.formats
      )
      return if (typedInstance is TypedCollection) {
         Flux.fromIterable(typedInstance.value)
      } else {
         Flux.fromIterable(listOf(typedInstance))
      }
   }

   private fun inferContentType(operation: RemoteOperation, headers: ClientResponse.Headers, result: String): Type {
      val mediaType: MediaType? = headers.contentType().orElse(null)
      when (mediaType) {
         // If we're consuming an event stream, and the return contract was a collection, we're actually consuming a single instance
         MediaType.TEXT_EVENT_STREAM -> return operation.returnType.collectionType ?: operation.returnType
         MediaType.APPLICATION_JSON -> return operation.returnType
      }
      return operation.returnType.collectionType ?: operation.returnType
   }
}
