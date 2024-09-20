package io.orbital.station

import com.orbitalhq.licensing.OrbitalLicenseServerApi
import com.orbitalhq.spring.config.LoadBalancerFilterFunction
import com.orbitalhq.spring.config.OrbitalOnly
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory


@Configuration
@OrbitalOnly
class OrbitalLicenseServerConfig {

   // This doesn't live in the LicenseConfig class, as we need access to the DicsoveryClient
   // and LoadBalancerFilterFunction, which are not dependencies in that project
   @Bean
   fun licenseServerApi(discoveryClient: DiscoveryClient): OrbitalLicenseServerApi {
      val webClient = WebClient.builder()
         .filter(LoadBalancerFilterFunction(discoveryClient))
         .build()

      val adapter = WebClientAdapter.create(webClient)
      val factory = HttpServiceProxyFactory.builderFor(adapter).build()

      return factory.createClient(OrbitalLicenseServerApi::class.java)
   }
}
