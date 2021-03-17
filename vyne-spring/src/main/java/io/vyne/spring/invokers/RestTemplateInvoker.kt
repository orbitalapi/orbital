package io.vyne.spring.invokers

import io.vyne.http.UriVariableProvider
import io.vyne.http.UriVariableProvider.Companion.buildRequestBody
import io.vyne.models.DataSource
import io.vyne.models.OperationResult
import io.vyne.models.TypedInstance
import io.vyne.models.UndefinedSource
import io.vyne.query.OperationType
import io.vyne.query.ProfilerOperation
import io.vyne.query.RemoteCall
import io.vyne.query.graph.operationInvocation.OperationInvoker
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Service
import io.vyne.schemas.httpOperationMetadata
import io.vyne.spring.hasHttpMetadata
import io.vyne.spring.isServiceDiscoveryClient
import io.vyne.utils.orElse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import lang.taxi.utils.log
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpRequest
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.ResponseErrorHandler
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.DefaultUriBuilderFactory
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import reactor.util.retry.Retry
import java.time.Duration

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

class RestTemplateInvoker(val schemaProvider: SchemaProvider,
                          val restTemplate: RestTemplate,
                          val webClient: WebClient,
                          private val serviceUrlResolvers: List<ServiceUrlResolver> = listOf(ServiceDiscoveryClientUrlResolver()),
                          private val enableDataLineageForRemoteCalls: Boolean) : OperationInvoker {

   @Autowired
   constructor(schemaProvider: SchemaProvider,
               restTemplateBuilder: RestTemplateBuilder,
               webClientBuilder: WebClient.Builder,
               serviceUrlResolvers: List<ServiceUrlResolver> = listOf(ServiceDiscoveryClientUrlResolver()),
               @Value("\${vyne.data-lineage.remoteCalls.enabled:false}") enableDataLineageForRemoteCalls: Boolean)
      : this(schemaProvider, restTemplateBuilder
      .errorHandler(CatchingErrorHandler())
      .additionalInterceptors(LoggingRequestInterceptor())
      .build(), webClientBuilder.exchangeStrategies(
         ExchangeStrategies.builder().codecs { it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024) }.build()
      ).build(), serviceUrlResolvers, enableDataLineageForRemoteCalls)

   private val uriVariableProvider = UriVariableProvider()
   private val defaultUriBuilderFactory = DefaultUriBuilderFactory()

   init {
      log().info("Rest template invoker starter")
   }

   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return service.isServiceDiscoveryClient() || operation.hasHttpMetadata()
   }


   override fun invoke(service: Service, operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>, profilerOperation: ProfilerOperation): Flow<TypedInstance> {
      log().debug("Invoking Operation ${operation.name} with parameters: ${parameters.joinToString(",") { (_, typedInstance) -> typedInstance.type.fullyQualifiedName + " -> " + typedInstance.toRawObject() }}")

      val (_, url, method) = operation.httpOperationMetadata()
      val httpMethod = HttpMethod.resolve(method)
      val httpResult = profilerOperation.startChild(this, "Invoke HTTP Operation", OperationType.REMOTE_CALL) { httpInvokeOperation ->

         val absoluteUrl = makeUrlAbsolute(service, operation, url)
         val uriVariables = uriVariableProvider.getUriVariables(parameters, url)

         log().debug("Operation ${operation.name} resolves to $absoluteUrl")
         httpInvokeOperation.addContext("AbsoluteUrl", absoluteUrl)

         val requestBody = buildRequestBody(operation, parameters.map { it.second })
         httpInvokeOperation.addContext("Service", service)
         httpInvokeOperation.addContext("Operation", operation)

         val expandedUri = defaultUriBuilderFactory.expand(absoluteUrl,uriVariables)


         //TODO - On upgrade to Spring boot 2.4.X replace usage of exchange with exchangeToFlow LENS-473
         webClient
            .method(httpMethod)
            .uri(absoluteUrl, uriVariables)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody.first.body)
            .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .metrics()
            .publishOn(Schedulers.elastic())
            .flatMapMany { clientResponse ->
               (

                  if (clientResponse.headers().contentType().orElse(MediaType.APPLICATION_JSON)
                        .isCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                  ) {
                     clientResponse.bodyToFlux(String::class.java)
                  } else {
                     // Assume the response is application/json

                     clientResponse.bodyToMono(typeReference<List<String>>())
                        .flatMapMany { Flux.fromIterable(it) }
                  }
                  )

                  .map {

                     //TODO - Should we create a remote call for each response?
                     val remoteCall = RemoteCall(
                        service.name,
                        expandedUri.toASCIIString(),
                        operation.name,
                        operation.returnType.name,
                        httpMethod.name,
                        requestBody.first.body,
                        clientResponse.rawStatusCode(),
                        httpInvokeOperation.duration,
                        it
                     )

                     handleSuccessfulHttpResponse(
                        it.toString(),
                        operation,
                        parameters,
                        remoteCall,
                        clientResponse.headers()
                     )

                  }

            }.retryWhen(
               Retry.backoff(
                  3,
                  Duration.ofMillis(100)
               )
            )
      }

      httpResult.doOnNext { println("Emitted response:") }.blockLast()

      return httpResult.asFlow()

   }

   private fun handleSuccessfulHttpResponse(result: String, operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>, remoteCall: RemoteCall, headers: ClientResponse.Headers): TypedInstance {
      // TODO : Handle scenario where we get a 2xx response, but no body
      log().debug("Result of ${operation.name} was $result")

      val isPreparsed = headers
         .header(io.vyne.http.HttpHeaders.CONTENT_PREPARSED).let { headerValues ->
            headerValues != null && headerValues.isNotEmpty() && headerValues.first() == true.toString()
         }
      // If the content has been pre-parsed upstream, we don't evaluate accessors
      val evaluateAccessors = !isPreparsed
      val dataSource = remoteCallDataLineage(parameters, remoteCall)
      println("Attempting to marshall result to TypedInstance ${result}")
      return TypedInstance.from(operation.returnType.collectionType!!, result, schemaProvider.schema(), source = dataSource, evaluateAccessors = evaluateAccessors)
   }


   private fun remoteCallDataLineage(parameters: List<Pair<Parameter, TypedInstance>>, remoteCall: RemoteCall): DataSource {
      return if (enableDataLineageForRemoteCalls) {
         // TODO: WE should be validating that the response we received conforms with the expected schema,
         // and doing...something? if it doesn't.
         // See https://gitlab.com/vyne/vyne/issues/54
         OperationResult(remoteCall, parameters.map { (param, instance) ->
            OperationResult.OperationParam(param.name.orElse("Unnamed"), instance)
         })
      } else {
         UndefinedSource
      }
   }

   private fun makeUrlAbsolute(service: Service, operation: RemoteOperation, url: String): String {
      return this.serviceUrlResolvers.first { it.canResolve(service, operation) }.makeAbsolute(url, service, operation)
   }
}

internal class CatchingErrorHandler : ResponseErrorHandler {
   override fun handleError(p0: ClientHttpResponse?) {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   override fun hasError(p0: ClientHttpResponse?): Boolean {
      return false
   }

}

internal class LoggingRequestInterceptor : ClientHttpRequestInterceptor {
   override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
      log().debug("Invoking ${request.method} on ${request.uri} with payload: ${String(body)}")
      return execution.execute(request, body)
   }

}
