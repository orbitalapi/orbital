package com.orbitalhq.copilot

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import org.springframework.web.service.invoker.createClient


/**
 * Marker object to import all things copilot
 */
@Configuration
@EnableConfigurationProperties(
   CopilotSettings::class
)
@ComponentScan(basePackageClasses = [CopilotSpringModule::class])
class CopilotSpringModule {

   @Bean
   fun copilotConversationApi(copilotSettings: CopilotSettings): CopilotConversationApi {
      val webClient = WebClient.create(copilotSettings.endpointUrl)
      val adapter = WebClientAdapter.create(webClient)
      val serviceFactory = HttpServiceProxyFactory.builderFor(adapter).build()
      val copilotApi: CopilotConversationApi = serviceFactory.createClient()
      return copilotApi
   }
}


@ConfigurationProperties(prefix = "vyne.copilot")
data class CopilotSettings(
   val endpointUrl: String = "http://localhost:9028",
)

