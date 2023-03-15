package io.vyne

import io.vyne.query.VyneJacksonModule
import io.vyne.remote.RemoteVyneClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.client.loadbalancer.LoadBalanced
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.client.WebClient

@Import(VyneClientConfiguration::class)
annotation class EnableRemoteVyneClient

class VyneClientConfiguration {
   @Value("\${vyne.queryServiceUrl}")
   lateinit var queryServiceUrl: String


   @LoadBalanced
   @Bean
   fun vyneWebClient(): WebClient.Builder {
      return WebClient.builder()
   }

   @Bean
   fun vyneJacksonModule() = VyneJacksonModule()

   @Bean
   fun vyneClient(vyneWebClientBuilder: WebClient.Builder): VyneClient {
      val queryService = WebClientVyneQueryService(vyneWebClientBuilder)
      return RemoteVyneClient(queryService)
   }
}

