package io.vyne.spring.invokers

import io.vyne.models.DataSource
import io.vyne.models.OperationResult
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.models.UndefinedSource
import io.vyne.query.OperationType
import io.vyne.query.ProfilerOperation
import io.vyne.query.RemoteCall
import io.vyne.query.graph.operationInvocation.OperationInvocationException
import io.vyne.query.graph.operationInvocation.OperationInvoker
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.Operation
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Service
import io.vyne.spring.hasHttpMetadata
import io.vyne.spring.isServiceDiscoveryClient
import io.vyne.utils.orElse
import lang.taxi.utils.log
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpRequest
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.ResponseErrorHandler
import org.springframework.web.client.RestTemplate


class RestTemplateInvoker(val schemaProvider: SchemaProvider,
                          val restTemplate: RestTemplate,
                          private val serviceUrlResolvers: List<ServiceUrlResolver> = listOf(ServiceDiscoveryClientUrlResolver()),
                          private val enableDataLineageForRemoteCalls: Boolean) : OperationInvoker {

   @Autowired
   constructor(schemaProvider: SchemaProvider,
               restTemplateBuilder: RestTemplateBuilder,
               serviceUrlResolvers: List<ServiceUrlResolver> = listOf(ServiceDiscoveryClientUrlResolver()),
               @Value("\${vyne.data-lineage.remoteCalls.enabled:false}") enableDataLineageForRemoteCalls: Boolean)
      : this(schemaProvider, restTemplateBuilder
      .errorHandler(CatchingErrorHandler())
      .additionalInterceptors(LoggingRequestInterceptor())
      .build(), serviceUrlResolvers, enableDataLineageForRemoteCalls)

   private val uriVariableProvider = UriVariableProvider()

   init {
      log().info("Rest template invoker starter")
   }

   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return service.isServiceDiscoveryClient() || operation.hasHttpMetadata()
   }


   override fun invoke(service: Service, operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>, profilerOperation: ProfilerOperation): TypedInstance {
      log().debug("Invoking Operation ${operation.name} with parameters: ${parameters.joinToString(",") { (_, typedInstance) -> typedInstance.type.fullyQualifiedName + " -> " + typedInstance.toRawObject() }}")

      val annotation = operation.metadata("HttpOperation")
      val httpMethod = HttpMethod.resolve(annotation.params["method"] as String)
      val url = annotation.params["url"] as String


      val httpResult = profilerOperation.startChild(this, "Invoke HTTP Operation", OperationType.REMOTE_CALL) { httpInvokeOperation ->
         val absoluteUrl = makeUrlAbsolute(service, operation, url)
         val uriVariables = uriVariableProvider.getUriVariables(parameters, url)

         log().debug("Operation ${operation.name} resolves to $absoluteUrl")
         httpInvokeOperation.addContext("AbsoluteUrl", absoluteUrl)

         val requestBody = buildRequestBody(operation, parameters.map { it.second })
         httpInvokeOperation.addContext("Service", service)
         httpInvokeOperation.addContext("Operation", operation)

         val start = System.currentTimeMillis()
         val result = restTemplate.exchange(absoluteUrl, httpMethod, requestBody.first, requestBody.second, uriVariables)
         val executionTime = System.currentTimeMillis() - start
         log().info("{} {} took {}ms with {}", httpMethod, absoluteUrl, executionTime, uriVariables)

         val expandedUri = restTemplate.uriTemplateHandler.expand(absoluteUrl, uriVariables)
         val remoteCall = RemoteCall(
            service.name, expandedUri.toASCIIString(),
            operation.name,
            operation.returnType.name,
            httpMethod.name, requestBody.first.body, result.statusCodeValue, httpInvokeOperation.duration, result.body
         )
         httpInvokeOperation.addRemoteCall(remoteCall)
         if (result.statusCode.is2xxSuccessful) {
            handleSuccessfulHttpResponse(result, operation, parameters, remoteCall)
         } else {
            handleFailedHttpResponse(result, operation, absoluteUrl, httpMethod, requestBody)
         }
      }
      return httpResult

   }

   private fun handleFailedHttpResponse(result: ResponseEntity<out Any>, operation: RemoteOperation, absoluteUrl: Any, httpMethod: HttpMethod, requestBody: Any): Nothing {
      val message = "Failed load invoke $httpMethod to $absoluteUrl - received $result"
      log().warn(message)
      throw OperationInvocationException(message)
   }

   private fun handleSuccessfulHttpResponse(result: ResponseEntity<out Any>, operation: RemoteOperation, parameters: List<Pair<Parameter, TypedInstance>>, remoteCall: RemoteCall): TypedInstance {
      // TODO : Handle scenario where we get a 2xx response, but no body
      log().debug("Result of ${operation.name} was $result")
      val resultBody = result.body
      val dataSource = remoteCallDataLineage(parameters, remoteCall)
      return TypedInstance.from(operation.returnType, resultBody, schemaProvider.schema(), source = dataSource)
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

   private fun buildRequestBody(operation: RemoteOperation, parameters: List<TypedInstance>): Pair<HttpEntity<*>, Class<*>> {
      if (operation.hasMetadata("HttpOperation")) {
         // TODO Revisit as this is a quick hack to invoke services that returns simple/text
         val httpOperation = operation.metadata("HttpOperation")
         httpOperation.params["consumes"]?.let {
            val httpHeaders = HttpHeaders()
            httpHeaders.accept = mutableListOf(MediaType.parseMediaType(it as String))
            return HttpEntity<String>(httpHeaders) to String::class.java
         }
      }
      val requestBodyParamIdx = operation.parameters.indexOfFirst { it.hasMetadata("RequestBody") }
      if (requestBodyParamIdx == -1) return HttpEntity.EMPTY to Any::class.java
      // TODO : For now, only looking up param based on type.  This is obviously naieve, and should
      // be improved, using name / position?  (note that parameters don't appear to be ordered in the list).

      val requestBodyParamType = operation.parameters[requestBodyParamIdx].type
      val requestBodyTypedInstance = parameters.first { it.type.name == requestBodyParamType.name }
      return HttpEntity(requestBodyTypedInstance.toRawObject()) to Any::class.java

   }

}

typealias ParameterValuePair = Pair<Parameter, TypedInstance>

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
