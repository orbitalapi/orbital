package io.vyne

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.client.loadbalancer.LoadBalanced
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient
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
   fun vyneRestTemplate():RestTemplate {
      return RestTemplate()
   }

   @Bean
   fun vyneClient(@Qualifier("vyneRestTemplate") restTemplate: RestTemplate, loadBalancer:LoadBalancerClient): VyneClient {
      val queryService = HttpVyneQueryService(queryServiceUrl, restTemplate)
      return VyneClient(queryService)
   }
}
