package com.orbitalhq.query.runtime.executor.serverless

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.function.adapter.aws.CustomRuntimeEventLoop
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
// Can't use ConditionalOnProperty when using a native image
//@ConditionalOnProperty(value = ["vyne.consumer.serverless.enabled"], havingValue = "true", matchIfMissing = false)
class ServerlessExecutorConfig {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   init {
      logger.info { "Configuring instance to consumer serverless functions" }
   }

   @Bean
   // Can't use Profiles when compiling a native image, so method returns null if not activated.
//   @Profile("aws")
   fun awsEventLoop(
      @Value("\${vyne.consumer.serverless.enabled:false}") enabled: Boolean,
      context: ConfigurableApplicationContext
   ): CustomRuntimeEventLoop? {
      return if (enabled) {
         logger.info { "Custom AWS runtime event loop initializing" }
         CustomRuntimeEventLoop(context)
      } else {
         // TODO :  Make Debug
         logger.info { "AWS Runtime not activated." }
         null
      }

   }


}
