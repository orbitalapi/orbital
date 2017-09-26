package io.polymer.spring.invokers

import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.models.TypedObject
import io.osmosis.polymer.query.graph.operationInvocation.OperationInvoker
import io.osmosis.polymer.schemas.Operation
import io.osmosis.polymer.schemas.Schema
import io.osmosis.polymer.schemas.Service
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate

class SpringMvcInvoker(val schema: Schema,
                       val restTemplate: RestTemplate = RestTemplate(),
                       val serviceUrlResolvers: List<ServiceUrlResolver> = listOf(ServiceDiscoveryClientUrlResolver())) : OperationInvoker {
   override fun canSupport(service: Service, operation: Operation): Boolean {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   override fun invoke(service: Service, operation: Operation, parameters: List<TypedInstance>): TypedInstance {
      val annotation = operation.metadata("HttpOperation")
      val httpMethod = HttpMethod.resolve(annotation.params["method"] as String)
      val url = annotation.params["url"] as String
      val absoluteUrl = makeUrlAbsolute(service, operation, url)

      val requestBody = buildRequestBody(operation, parameters)
      val result = restTemplate.exchange(absoluteUrl, httpMethod, requestBody, Any::class.java, getUriVariables(parameters))
      if (result.statusCode.is2xxSuccessful) {
         // TODO : Handle scenario where we get a 2xx response, but no body
         if (result.body is Map<*, *>) {
            return TypedObject.fromAttributes(operation.returnType, result.body as Map<String, Any>, schema)
         } else {
            return TypedInstance.from(operation.returnType, result.body, schema)
         }
      } else {
         TODO("Handle failed calls")
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
      return HttpEntity(requestBodyTypedInstance.asMap())

   }

   private fun getUriVariables(parameters: List<TypedInstance>): Map<String, Any> {
      return parameters.map { it.type.fullyQualifiedName to it.value }.toMap()
   }

}
