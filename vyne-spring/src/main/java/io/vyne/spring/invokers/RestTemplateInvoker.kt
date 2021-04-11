package io.vyne.spring.invokers

import io.vyne.http.HttpHeaders.STREAM_ESTIMATED_RECORD_COUNT
import io.vyne.http.UriVariableProvider
import io.vyne.http.UriVariableProvider.Companion.buildRequestBody
import io.vyne.models.OperationResult
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.query.ProfilerOperation
import io.vyne.query.RemoteCall
import io.vyne.query.active.ActiveQueryMonitor
import io.vyne.query.graph.operationInvocation.OperationInvocationException
import io.vyne.query.graph.operationInvocation.OperationInvoker
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.*
import io.vyne.spring.hasHttpMetadata
import io.vyne.spring.isServiceDiscoveryClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.reactive.asFlow
import lang.taxi.utils.log
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.util.DefaultUriBuilderFactory
import reactor.core.publisher.Flux
import java.util.*

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

class RestTemplateInvoker(
   val schemaProvider: SchemaProvider,
   val webClient: WebClient,
   private val serviceUrlResolvers: List<ServiceUrlResolver> = ServiceUrlResolver.DEFAULT,
   private val activeQueryMonitor: ActiveQueryMonitor
) : OperationInvoker {

   @Autowired
   constructor(
      schemaProvider: SchemaProvider,
      webClientBuilder: WebClient.Builder,
      serviceUrlResolvers: List<ServiceUrlResolver> = listOf(ServiceDiscoveryClientUrlResolver()),
      activeQueryMonitor: ActiveQueryMonitor
   )
      : this(
      schemaProvider, webClientBuilder.exchangeStrategies(
         ExchangeStrategies.builder().codecs { it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024) }.build()
      ).build(), serviceUrlResolvers,
      activeQueryMonitor
   )

   private val uriVariableProvider = UriVariableProvider()
   private val defaultUriBuilderFactory = DefaultUriBuilderFactory()

   init {
      log().info("Rest template invoker starter")
   }

   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return service.isServiceDiscoveryClient() || operation.hasHttpMetadata()
   }


   override fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      profilerOperation: ProfilerOperation, queryId: String?
   ): Flow<TypedInstance> {
      log().debug("Invoking Operation ${operation.name} with parameters: ${parameters.joinToString(",") { (_, typedInstance) -> typedInstance.type.fullyQualifiedName + " -> " + typedInstance.toRawObject() }}")


      val (_, url, method) = operation.httpOperationMetadata()
      val httpMethod = HttpMethod.resolve(method)!!
      //val httpResult = profilerOperation.startChild(this, "Invoke HTTP Operation", OperationType.REMOTE_CALL) { httpInvokeOperation ->

      val absoluteUrl = makeUrlAbsolute(service, operation, url)
      val uriVariables = uriVariableProvider.getUriVariables(parameters, url)

      log().debug("Operation ${operation.name} resolves to $absoluteUrl")
      val requestBody = buildRequestBody(operation, parameters.map { it.second })

      val expandedUri = defaultUriBuilderFactory.expand(absoluteUrl, uriVariables)

      //TODO - On upgrade to Spring boot 2.4.X replace usage of exchange with exchangeToFlow LENS-473
      val request = webClient
         .method(httpMethod)
         .uri(absoluteUrl, uriVariables)
         .contentType(MediaType.APPLICATION_JSON)
      if (requestBody.first.hasBody()) {
         request.bodyValue(requestBody.first.body)
      }

      val remoteCallId = UUID.randomUUID().toString()
      val results = request
         .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON)
         .exchange()
         .metrics()
         .elapsed()
         .flatMapMany { durationAndResponse ->
            val duration = durationAndResponse.t1
            val clientResponse = durationAndResponse.t2
            if (clientResponse.statusCode().isError) {
               throw OperationInvocationException("Error invoking URL $expandedUri", clientResponse.statusCode())
            }
            reportEstimatedResults(queryId, clientResponse.headers())
            clientResponse.bodyToFlux<String>()
               .flatMap { responseString ->
                  val remoteCall = RemoteCall(
                     remoteCallId = remoteCallId,
                     service = service.name,
                     address = expandedUri.toASCIIString(),
                     operation = operation.name,
                     responseTypeName = operation.returnType.name,
                     method = httpMethod.name,
                     requestBody = requestBody.first.body,
                     resultCode = clientResponse.rawStatusCode(),
                     durationMs = duration,
                     response = responseString
                  )

                  handleSuccessfulHttpResponse(
                     responseString,
                     operation,
                     parameters,
                     remoteCall,
                     clientResponse.headers()
                  )

               }
         }
      return results.asFlow().flowOn(Dispatchers.IO)

   }

   private fun reportEstimatedResults(queryId: String?, headers: ClientResponse.Headers) {
      if (queryId == null) {
         return
      }
      if (headers.header(STREAM_ESTIMATED_RECORD_COUNT).isNotEmpty()) {
         activeQueryMonitor.incrementExpectedRecordCount(
            queryId,
            Integer.valueOf(headers.header(STREAM_ESTIMATED_RECORD_COUNT)[0])
         )
      }
   }

   private fun handleSuccessfulHttpResponse(
      result: String,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      remoteCall: RemoteCall,
      headers: ClientResponse.Headers
   ): Flux<TypedInstance> {
      // TODO : Handle scenario where we get a 2xx response, but no body

      log().debug("Result of ${operation.name} was $result")

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
         schemaProvider.schema(),
         source = dataSource,
         evaluateAccessors = evaluateAccessors
      )
      return if (typedInstance is TypedCollection) {
         return Flux.fromIterable(typedInstance.value)
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
