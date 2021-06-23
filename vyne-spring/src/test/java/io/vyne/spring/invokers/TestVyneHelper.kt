package io.vyne.spring.invokers

import io.vyne.Vyne
import io.vyne.query.graph.operationInvocation.CacheAwareOperationInvocationDecorator
import io.vyne.schemaStore.SchemaProvider
import org.springframework.web.reactive.function.client.WebClient

enum class Invoker {
   RestTemplateInvoker,
   CachingInvoker

}
fun testVyne(schema: String, invoker:Invoker): Vyne {
   return io.vyne.testVyne(schema) { schema ->
      val invoker = RestTemplateInvoker(
         webClient = WebClient.builder().build(),
         schemaProvider = SchemaProvider.from(schema)
      ).let {
         if (invoker == Invoker.CachingInvoker) {
            CacheAwareOperationInvocationDecorator(it)
         } else {
            it
         }
      }
      listOf(invoker)
   }
}

