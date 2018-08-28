package io.vyne

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@Import(VyneClientConfiguration::class)
annotation class EnableVyneClient {

}

class VyneClientConfiguration {

   @Value("\${vyne.queryServiceUrl}")
   lateinit var queryServiceUrl: String

   @Bean
   fun vyneClient(): VyneClient {
      return VyneClient(queryServiceUrl)
   }
}
