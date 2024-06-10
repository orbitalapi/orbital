package com.orbitalhq.copilot

import okhttp3.OkHttpClient
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit


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
   fun chatGptService(copilotSettings: CopilotSettings): OpenAiChatService {
      return OpenAiChatService(
         copilotSettings.apiKey,
         copilotSettings.endpointUrl,
         copilotSettings.model,
         OkHttpClient().newBuilder().readTimeout(30, TimeUnit.SECONDS).build()
      )
   }

}
