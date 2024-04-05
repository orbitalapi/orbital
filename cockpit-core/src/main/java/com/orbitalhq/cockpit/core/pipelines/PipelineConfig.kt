package com.orbitalhq.cockpit.core.pipelines

import com.orbitalhq.pipelines.jet.api.PipelineApi
import com.orbitalhq.spring.config.LoadBalancerFilterFunction
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory


@ConfigurationProperties(prefix = "vyne.pipelines")
data class PipelineConfig(
   val kibanaUrl: String? = null,
   val logsIndex: String? = null
)

@Configuration
class PipelineSpringConfig {

   @Bean
   fun pipelineApi(
      discoveryClient: DiscoveryClient,
      ): PipelineApi {
      val webClient = WebClient.builder()
         .baseUrl("http://stream-server/")
         .filter(LoadBalancerFilterFunction(discoveryClient))
         .build()

      val adapter = WebClientAdapter.create(webClient)
      val factory = HttpServiceProxyFactory.builderFor(adapter).build()

      val pipelineApi: PipelineApi = factory.createClient(PipelineApi::class.java)
      return pipelineApi
   }
}
