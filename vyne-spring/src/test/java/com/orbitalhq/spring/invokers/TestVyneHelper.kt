package com.orbitalhq.spring.invokers

import com.orbitalhq.Vyne
import com.orbitalhq.query.connectors.CacheAwareOperationInvocationDecorator
import com.orbitalhq.query.graph.operationInvocation.cache.local.LocalOperationCacheProvider
import com.orbitalhq.schema.api.SimpleSchemaProvider
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient

enum class Invoker {
   RestTemplate,
   RestTemplateWithCache

}

fun testVyne(schema: String, invoker: Invoker): Vyne {
   return com.orbitalhq.testVyne(schema) { schema ->
      val invoker = RestTemplateInvoker(
         webClient = WebClient.builder()
            .exchangeStrategies(ExchangeStrategies.builder()
               .codecs { config -> config.defaultCodecs().maxInMemorySize(2 * 1024 * 1024) }
               .build()
            ).build(),
         schemaProvider = SimpleSchemaProvider(schema)
      ).let {
         if (invoker == Invoker.RestTemplateWithCache) {
            CacheAwareOperationInvocationDecorator(it, LocalOperationCacheProvider.default())
         } else {
            it
         }
      }
      listOf(invoker)
   }
}
