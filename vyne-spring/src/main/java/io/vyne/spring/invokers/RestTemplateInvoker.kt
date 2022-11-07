package io.vyne.spring.invokers

import io.netty.channel.ChannelOption
import io.vyne.http.HttpHeaders.STREAM_ESTIMATED_RECORD_COUNT
import io.vyne.http.UriVariableProvider
import io.vyne.models.OperationResult
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.query.QueryContextEventDispatcher
import io.vyne.query.RemoteCall
import io.vyne.query.ResponseMessageType
import io.vyne.query.connectors.OperationInvoker
import io.vyne.schema.consumer.SchemaStore
import io.vyne.schemas.OperationInvocationException
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Service
import io.vyne.schemas.Type
import io.vyne.schemas.httpOperationMetadata
import io.vyne.spring.hasHttpMetadata
import io.vyne.spring.http.DefaultRequestFactory
import io.vyne.spring.http.HttpRequestFactory
import io.vyne.spring.isServiceDiscoveryClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.DefaultUriBuilderFactory
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

class RestTemplateInvoker(
    val schemaStore: SchemaStore,
    val webClient: WebClient,
    private val serviceUrlResolvers: List<ServiceUrlResolver> = ServiceUrlResolver.DEFAULT,
    private val requestFactory: HttpRequestFactory = DefaultRequestFactory()
) : OperationInvoker {
   private val logger = KotlinLogging.logger {}

   @Autowired
   constructor(
       schemaStore: SchemaStore,
       webClientBuilder: WebClient.Builder,
       serviceUrlResolvers: List<ServiceUrlResolver> = listOf(ServiceDiscoveryClientUrlResolver()),
       requestFactory: HttpRequestFactory = DefaultRequestFactory()
   )
      : this(
      schemaStore,
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
         .build(),

      serviceUrlResolvers,
      requestFactory
   )

   private val uriVariableProvider = UriVariableProvider()
   private val defaultUriBuilderFactory = DefaultUriBuilderFactory()

   init {
      logger.info { "Rest template invoker started" }
   }

   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return service.isServiceDiscoveryClient() || operation.hasHttpMetadata()
   }

   override suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String?
   ): Flow<TypedInstance> {
      logger.debug { "Invoking Operation ${operation.name} with parameters: ${parameters.joinToString(",") { (_, typedInstance) -> typedInstance.type.fullyQualifiedName + " -> " + typedInstance.toRawObject() }}" }

      val (_, url, method) = operation.httpOperationMetadata()
      val httpMethod = HttpMethod.resolve(method)!!
      //val httpResult = profilerOperation.startChild(this, "Invoke HTTP Operation", OperationType.REMOTE_CALL) { httpInvokeOperation ->

      val absoluteUrl = makeUrlAbsolute(service, operation, url)
      val uriVariables = uriVariableProvider.getUriVariables(parameters, url)

      logger.debug { "Operation ${operation.name} resolves to $absoluteUrl" }
      val typeInstanceParameters = parameters.map { it.second }
      val httpEntity = requestFactory.buildRequestBody(operation, typeInstanceParameters)
      val queryParams =  requestFactory.buildRequestQueryParams(operation)

      val expandedUri = defaultUriBuilderFactory.expand(absoluteUrl, uriVariables)

      //TODO - On upgrade to Spring boot 2.4.X replace usage of exchange with exchangeToFlow LENS-473
      val request = webClient
         .method(httpMethod)
         .uri { _ ->
            val uriBuilder = UriComponentsBuilder
               .fromUriString(absoluteUrl)
            (queryParams?.let { uriBuilder.queryParams(it) } ?: uriBuilder).build(uriVariables)
         }
         .contentType(MediaType.APPLICATION_JSON)
         .headers { consumer ->
            consumer.addAll(httpEntity.headers)
         }
      if (httpEntity.hasBody()) {
         request.bodyValue(httpEntity.body)
      }

      logger.debug { "[$queryId] - Performing $httpMethod to ${expandedUri.toASCIIString()}" }

      val remoteCallId = UUID.randomUUID().toString()
      val results = request
         .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON)
         .exchange()
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

            logger.debug { "[$queryId] - $httpMethod to ${expandedUri.toASCIIString()} returned status ${clientResponse.statusCode()} and body length of ${clientResponse.headers().contentLength().orElse(-1)} after ${duration}ms" }

            fun remoteCall(responseBody: String, failed: Boolean = false): RemoteCall {
               return RemoteCall(
                  remoteCallId = remoteCallId,
                  responseId = UUID.randomUUID().toString(),
                  service = service.name,
                  address = expandedUri.toASCIIString(),
                  operation = operation.name,
                  responseTypeName = operation.returnType.name,
                  method = httpMethod.name,
                  requestBody = httpEntity.body,
                  resultCode = clientResponse.rawStatusCode(),
                  durationMs = duration,
                  response = responseBody,
                  timestamp = initiationTime,
                  responseMessageType = responseMessageType,
                  isFailed = failed
               )
            }

            if (clientResponse.statusCode().isError) {
               return@flatMapMany clientResponse.bodyToMono<String>()
                  .switchIfEmpty(Mono.just(""))
                  .map { responseBody ->
                     val remoteCall = remoteCall(responseBody = responseBody, failed = true)
                     throw OperationInvocationException(
                        "Http error ${clientResponse.statusCode()} from url $expandedUri",
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
                        eventDispatcher
                     )
                  }
            } else {
               logger.debug { "Request to ${expandedUri.toASCIIString()} is not streaming" }
               if (!firstResultReceived) {
                  logger.debug { "Received body of non-streaming response" }
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
                        eventDispatcher
                     )
                  }
            }
         }

      return results.asFlow().flowOn(Dispatchers.IO)

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
      eventDispatcher: QueryContextEventDispatcher
   ): Flux<TypedInstance> {
      // Logging responses in our logs is a security issue.  Let's not do this.
//      logger.debug { "Result of ${operation.name} was $result" }

      val isPreparsed = headers
         .header(io.vyne.http.HttpHeaders.CONTENT_PREPARSED).let { headerValues ->
            headerValues != null && headerValues.isNotEmpty() && headerValues.first() == true.toString()
         }
      // If the content has been pre-parsed upstream, we don't evaluate accessors
      val evaluateAccessors = !isPreparsed
      val dataSource = OperationResult.from(parameters, remoteCall)

      val type = inferContentType(operation, headers, result)

      val typedInstance = TypedInstance.from(
         type,
         result,
         schemaStore.schemaSet.schema,
         source = dataSource,
         evaluateAccessors = evaluateAccessors
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


   private fun makeUrlAbsolute(service: Service, operation: RemoteOperation, url: String): String {
      return this.serviceUrlResolvers.firstOrNull { it.canResolve(service, operation) }
         ?.makeAbsolute(url, service, operation)
         ?: error("No url resolvers were found that can make url $url (on operation ${operation.qualifiedName}) absolute")
   }
}
