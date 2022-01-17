package io.vyne.spring.invokers

import io.vyne.Vyne
import io.vyne.query.graph.operationInvocation.CacheAwareOperationInvocationDecorator
import io.vyne.schemaApi.SchemaProvider
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient

enum class Invoker {
   RestTemplate,
   RestTemplateWithCache

}

fun testVyne(schema: String, invoker: Invoker): Vyne {
   return io.vyne.testVyne(schema) { schema ->
      val invoker = RestTemplateInvoker(
         webClient = WebClient.builder()
            .exchangeStrategies(ExchangeStrategies.builder()
               .codecs { config -> config.defaultCodecs().maxInMemorySize(2 * 1024 * 1024) }
               .build()
            ).build(),
         schemaProvider = SchemaProvider.from(schema)
      ).let {
         if (invoker == Invoker.RestTemplateWithCache) {
            CacheAwareOperationInvocationDecorator(it)
         } else {
            it
         }
      }
      listOf(invoker)
   }
}

