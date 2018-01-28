package io.polymer.spring.invokers

import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.models.TypedObject
import io.osmosis.polymer.query.graph.operationInvocation.OperationInvocationException
import io.osmosis.polymer.query.graph.operationInvocation.OperationInvoker
import io.osmosis.polymer.schemas.Operation
import io.osmosis.polymer.schemas.Service
import io.polymer.schemaStore.SchemaProvider
import lang.taxi.utils.log
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpRequest
import org.springframework.http.ResponseEntity
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.ResponseErrorHandler
import org.springframework.web.client.RestTemplate

class RestTemplateInvoker(val schemaProvider: SchemaProvider,
                          val restTemplate: RestTemplate,
                          private val serviceUrlResolvers: List<ServiceUrlResolver> = listOf(ServiceDiscoveryClientUrlResolver())) : OperationInvoker {

   @Autowired constructor(schemaProvider: SchemaProvider,
                          restTemplateBuilder: RestTemplateBuilder,
                          serviceUrlResolvers: List<ServiceUrlResolver> = listOf(ServiceDiscoveryClientUrlResolver()))
      : this(schemaProvider,restTemplateBuilder
      .errorHandler(CatchingErrorHandler())
      .additionalInterceptors(LoggingRequestInterceptor())
      .build(), serviceUrlResolvers)

   init {
      log().info("Rest template invoker starter")
   }
   override fun canSupport(service: Service, operation: Operation): Boolean {
      return service.hasMetadata("ServiceDiscoveryClient")
   }

   override fun invoke(service: Service, operation: Operation, parameters: List<TypedInstance>): TypedInstance {
      log().debug("Invoking Operation ${operation.name} with parameters: ${parameters.joinToString(",")}")

      val annotation = operation.metadata("HttpOperation")
      val httpMethod = HttpMethod.resolve(annotation.params["method"] as String)
      val url = annotation.params["url"] as String
      val absoluteUrl = makeUrlAbsolute(service, operation, url)
      log().debug("Operation ${operation.name} resolves to $absoluteUrl")

      val requestBody = buildRequestBody(operation, parameters)
      val result = restTemplate.exchange(absoluteUrl, httpMethod, requestBody, Any::class.java, getUriVariables(parameters))
      if (result.statusCode.is2xxSuccessful) {
         return handleSuccessfulHttpResponse(result, operation)
      } else {
         handleFailedHttpResponse(result,operation, absoluteUrl, httpMethod, requestBody)
         throw RuntimeException("Shouldn't hit this point")
      }
   }

   private fun handleFailedHttpResponse(result: ResponseEntity<Any>, operation: Operation, absoluteUrl: Any, httpMethod: HttpMethod, requestBody: Any) {
      val message = "Failed load invoke $httpMethod to $absoluteUrl - received $result"
      log().error(message)
      throw OperationInvocationException(message)
   }

   private fun handleSuccessfulHttpResponse(result: ResponseEntity<Any>, operation: Operation): TypedInstance {
      // TODO : Handle scenario where we get a 2xx response, but no body
      log().debug("Result of ${operation.name} was $result")
      if (result.body is Map<*, *>) {
         return TypedObject.fromAttributes(operation.returnType, result.body as Map<String, Any>, schemaProvider.schema())
      } else {
         return TypedInstance.from(operation.returnType, result.body, schemaProvider.schema())
      }
   }

   private fun makeUrlAbsolute(service: Service, operation: Operation, url: String): String {
      return this.serviceUrlResolvers.first { it.canResolve(service, operation) }.makeAbsolute(url, service, operation)
   }

   private fun buildRequestBody(operation: Operation, parameters: List<TypedInstance>): HttpEntity<*> {
      val requestBodyParamIdx = operation.parameters.indexOfFirst { it.hasMetadata("RequestBody") }
      if (requestBodyParamIdx == -1) return HttpEntity.EMPTY


      // TODO : For now, only looking up param based on type.  This is obviously naieve, and should
      // be improved, using name / position?  (note that parameters don't appear to be ordered in the list).

      val requestBodyParamType = operation.parameters[requestBodyParamIdx].type
      val requestBodyTypedInstance = parameters.first { it.type == requestBodyParamType }
      return HttpEntity(requestBodyTypedInstance.toRawObject())

   }

   private fun getUriVariables(parameters: List<TypedInstance>): Map<String, Any> {
      return parameters.map { it.type.fullyQualifiedName to it.value }.toMap()
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
      return execution.execute(request,body)
   }

}
