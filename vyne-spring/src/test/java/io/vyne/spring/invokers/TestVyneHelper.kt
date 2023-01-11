package io.vyne.spring.invokers

import io.vyne.*
import io.vyne.query.graph.operationInvocation.CacheAwareOperationInvocationDecorator
import io.vyne.schema.api.SchemaProvider
import io.vyne.schema.api.SchemaSet
import io.vyne.schemaStore.SimpleSchemaStore
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
         schemaStore = SimpleSchemaStore().createPackageAndSetSchema(schema.sources)
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

fun SimpleSchemaStore.createPackageAndSetSchema(
   sources: List<VersionedSource>,
   generation: Int = 1
): SimpleSchemaStore {
   return this.setSchemaSet(SchemaSet.fromParsed(listOf(
      ParsedPackage(PackageMetadata.from("io.vyne", "test", "1.0.0"),
         sources.map { ParsedSource(it) }
      )), 1))
}
