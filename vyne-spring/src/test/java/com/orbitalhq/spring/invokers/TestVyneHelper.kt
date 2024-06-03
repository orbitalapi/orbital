package com.orbitalhq.spring.invokers

import com.orbitalhq.Vyne
import com.orbitalhq.annotations.http.HttpRetryAnnotationSchema
import com.orbitalhq.query.connectors.CacheAwareOperationInvocationDecorator
import com.orbitalhq.query.graph.operationInvocation.cache.local.LocalCachingInvokerProvider
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.spring.http.auth.schemes.AuthWebClientCustomizer
import org.springframework.web.reactive.function.client.WebClient

enum class Invoker {
   RestTemplate,
   RestTemplateWithCache

}

fun testVyne(schema: String, invoker: Invoker): Vyne {
   val schemeWithRetryImport = StringBuilder().appendLine(HttpRetryAnnotationSchema.imports).appendLine(schema).toString()
   val schemas = listOf(
      HttpRetryAnnotationSchema.schema, schemeWithRetryImport)
   return com.orbitalhq.testVyne(schemas) { taxi ->
      val restTemplateInvoker = RestTemplateInvoker(
         webClientFactory = WebClientFactory(WebClient.builder(), AuthWebClientCustomizer.empty()),
         schemaProvider = SimpleSchemaProvider(taxi)
      ).let {
         if (invoker == Invoker.RestTemplateWithCache) {
            CacheAwareOperationInvocationDecorator(it, LocalCachingInvokerProvider.default())
         } else {
            it
         }
      }
      listOf(restTemplateInvoker)
   }
}
