package io.vyne

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.client.loadbalancer.LoadBalanced
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestTemplate

@Import(VyneClientConfiguration::class)
annotation class EnableVyneClient {

}

class VyneClientConfiguration {

   @Value("\${vyne.queryServiceUrl}")
   lateinit var queryServiceUrl: String

   @LoadBalanced
   @Bean
   fun vyneRestTemplate(): RestTemplate {
      return RestTemplate()
   }

   @Bean
   fun vyneClient(@Qualifier("vyneRestTemplate") restTemplate: RestTemplate, objectMapper: ObjectMapper, @Autowired(required = false) factProviders: List<FactProvider>?): VyneClient {
      val queryService = HttpVyneQueryService(queryServiceUrl, restTemplate)
      val myFactProviders = factProviders ?: emptyList()
      return VyneClient(queryService, myFactProviders, objectMapper)
   }
}
