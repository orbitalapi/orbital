package com.orbitalhq.spring.invokers

import com.orbitalhq.http.HttpHeaderNames.STREAM_ESTIMATED_RECORD_COUNT
import com.orbitalhq.http.UriVariableProvider
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedInstance.Companion.EXPIRY_METADATA
import com.orbitalhq.query.HttpExchange
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.OperationInvocationException
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.fqn
import com.orbitalhq.schemas.httpOperationMetadata
import com.orbitalhq.schemas.ignoreErrorsSpec
import com.orbitalhq.schemas.retrySpec
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
import lang.taxi.utils.log
import mu.KotlinLogging
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.DefaultUriBuilderFactory
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.switchIfEmpty
import java.net.URI
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}
class RestTemplateInvoker(
   val schemaProvider: SchemaProvider,
   private val webClientFactory: WebClientFactory,
   private val requestFactory: HttpRequestFactory = DefaultRequestFactory(FormatSpecRegistry.default().formats),
   val formats: FormatSpecRegistry = FormatSpecRegistry.default(),
) : OperationInvoker {
   companion object {
      val defaultAcceptHeaderValue = listOf(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)
   }


   constructor(
      schemaProvider: SchemaProvider,
      webClientBuilder: WebClient.Builder,
      authRequestCustomizer: AuthWebClientCustomizer,
      requestFactory: HttpRequestFactory = DefaultRequestFactory(FormatSpecRegistry.default().formats)
   )
      : this(
      schemaProvider,
      WebClientFactory(webClientBuilder, authRequestCustomizer),
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
      queryId: String,
      queryOptions: QueryOptions
   ): Flow<TypedInstance> {
      logger.info { "Invoking Operation ${operation.name} with parameters: ${parameters.joinToString(",") { (_, typedInstance) -> typedInstance.type.fullyQualifiedName + " -> " + typedInstance.toRawObject() }}" }

      val (_, url, method) = operation.httpOperationMetadata()
      val retrySpec = operation.retrySpec()
      val httpMethod = HttpMethod.valueOf(method)
      //val httpResult = profilerOperation.startChild(this, "Invoke HTTP Operation", OperationType.REMOTE_CALL) { httpInvokeOperation ->

      val absoluteUrl = prependServiceBaseUrl(service, url)
      val uriVariables = uriVariableProvider.getUriVariables(parameters, absoluteUrl)

      logger.info { "Operation ${operation.name} resolves to $absoluteUrl" }
      val typeInstanceParameters = parameters.map { it.second }
      val httpEntity = requestFactory.buildRequestBody(operation, typeInstanceParameters)
      val queryParams = requestFactory.buildRequestQueryParams(parameters)

      val expandedUri = defaultUriBuilderFactory.expand(absoluteUrl, uriVariables)

      val webClient = webClientFactory.webClientFor(service)

      //TODO - On upgrade to Spring boot 2.4.X replace usage of exchange with exchangeToFlow LENS-473
      val request = webClient
         .method(httpMethod)
         .uri { _ ->
            val uriBuilder = UriComponentsBuilder
               .fromUriString(absoluteUrl)
            (queryParams?.let { uriBuilder.queryParams(it) } ?: uriBuilder).build(uriVariables)
         }
         .headers { consumer ->
            if (httpEntity.headers.accept.isEmpty()) {
               // If Accept header value is not explicitly specified, set it to default Orbital values.
               consumer.accept = defaultAcceptHeaderValue
            }
            consumer.addAll(httpEntity.headers)
         }
         .addAuthTokenAttributes(service.name.fullyQualifiedName)
      if (httpEntity.hasBody()) {
         request.bodyValue(httpEntity.body)
      }


      logger.info { "[$queryId] - Performing $httpMethod to ${expandedUri.toASCIIString()} with retry specification => ${retrySpec?.toLogString()}" }

      val remoteCallId = UUID.randomUUID().toString()

      val results = request
         .exchange()
         .onErrorMap { error -> mapError(
            error,
            remoteCallId,
            service,
            operation,
            httpEntity,
            httpMethod,
            expandedUri,
            eventDispatcher,
            absoluteUrl,
            queryId,
            parameters) }
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
                     headers = com.orbitalhq.query.HttpHeaders(
                        responseHeaders = clientResponse.headers().asHttpHeaders().asVyneHeadersMap(),
                        requestHeaders = clientResponse.requestHeaders()
                     )
                  )
               )
            }

            if (clientResponse.statusCode().isError) {
               if (retrySpec != null && retrySpec.responseCodes.contains(clientResponse.statusCode().value())) {
                  return@flatMapMany Mono.error(RestRetryException("retry", clientResponse.statusCode().value()))
               }

               val httpIgnoreErrorSpec = operation.ignoreErrorsSpec()
               if (httpIgnoreErrorSpec == null || !httpIgnoreErrorSpec.match(clientResponse.statusCode().value())) {
                  return@flatMapMany clientResponse.bodyToMono<String>()
                     .switchIfEmpty(Mono.just(""))
                     .map { responseBody ->
                        val remoteCall = remoteCall(responseBody = responseBody, failed = true)
                        eventDispatcher.reportRemoteOperationInvoked(
                           OperationResult.from(parameters, remoteCall),
                           queryId
                        )
                        throw OperationInvocationException(
                           "http error ${clientResponse.statusCode()} from url $expandedUri - $responseBody",
                           clientResponse.statusCode().value(),
                           remoteCall,
                           parameters
                        )
                     }
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
                  .switchIfEmpty { Mono.just("") } // 204 responses (no content)
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

      return if (retrySpec != null) {
         results
            .retryWhen(retrySpec.retrySpec)
            .onErrorMap { error -> mapError(
               error,
               remoteCallId,
               service,
               operation,
               httpEntity,
               httpMethod,
               expandedUri,
               eventDispatcher,
               absoluteUrl,
               queryId,
               parameters) }
            .asFlow().flowOn(Dispatchers.IO)
      } else {
         results.asFlow().flowOn(Dispatchers.IO)
      }
   }

   private fun mapError(error: Throwable,
                        remoteCallId: String,
                        service: Service,
                        operation: RemoteOperation,
                        httpEntity: HttpEntity<*>,
                        httpMethod: HttpMethod,
                        expandedUri: URI,
                        eventDispatcher: QueryContextEventDispatcher,
                        absoluteUrl: String,
                        queryId: String,
                        parameters: List<Pair<Parameter, TypedInstance>>
   ): Throwable {
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
         response = error.message,
         timestamp = Instant.now(),
         responseMessageType = ResponseMessageType.FULL,
         isFailed = true,
         exchange = HttpExchange(
            url = expandedUri.toASCIIString(),
            verb = httpMethod.name(),
            requestBody = httpEntity.body?.toString(),
            responseCode = -1,
            responseSize = 0,
            headers = com.orbitalhq.query.HttpHeaders.empty()
         )
      )
      eventDispatcher.reportRemoteOperationInvoked(OperationResult.from(parameters, remoteCall), queryId)
      return OperationInvocationException(
         "Failed to invoke service ${operation.name} at url $absoluteUrl - ${error.message ?: "No message in instance of ${error::class.simpleName}"}",
         0,
         remoteCall,
         parameters
      )
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

      val itemExpiredAt = HttpCacheControl.parse(headers.asHttpHeaders())?.expiredAt()
      val isPreparsed = headers
         .header(com.orbitalhq.http.HttpHeaderNames.CONTENT_PREPARSED).let { headerValues ->
             headerValues.isNotEmpty() && headerValues.first() == true.toString()
         }
      // If the content has been pre-parsed upstream, we don't evaluate accessors
      val evaluateAccessors = !isPreparsed
      val operationResult = OperationResult.from(parameters, remoteCall)

      val type = inferContentType(operation, headers)
      eventDispatcher.reportRemoteOperationInvoked(operationResult, queryId)

      val metadata: Map<String, Any> = itemExpiredAt?.let {
         mapOf(EXPIRY_METADATA to it)
      } ?: emptyMap()

      val typedInstance = TypedInstance.from(
         type,
         result,
         schemaProvider.schema,
         source = operationResult.asOperationReferenceDataSource(),
         evaluateAccessors = evaluateAccessors,
         formatSpecs = formats.formats,
         metadata = metadata
      )
      return if (typedInstance is TypedCollection) {
         Flux.fromIterable(typedInstance.value)
      } else {
         Flux.fromIterable(listOf(typedInstance))
      }
   }

   private fun inferContentType(operation: RemoteOperation, headers: ClientResponse.Headers): Type {
      val mediaType: MediaType? = headers.contentType().orElse(null)
      when (mediaType) {
         // If we're consuming an event stream, and the return contract was a collection, we're actually consuming a single instance
         MediaType.TEXT_EVENT_STREAM -> return operation.returnType.collectionType ?: operation.returnType
         MediaType.APPLICATION_JSON -> return operation.returnType
      }
      return operation.returnType.collectionType ?: operation.returnType
   }
}

class RestRetryException(
   message: String,
   val httpStatus: Int
) : RuntimeException(message)


fun HttpHeaders.asVyneHeadersMap():Map<String,List<String>> {
   return this.toMutableMap().toMap()
}

fun ClientResponse.requestHeaders():Map<String,List<String>> {
   return when (this) {
      is ResponseWithRequestHeaders -> this.requestHeaders.asVyneHeadersMap()
      else -> {
         log().debug("Called ClientResponse.requestHeaders(), but the request headers weren't captured - got instance of ${this::class.simpleName}")
         emptyMap()
      }
   }
}
