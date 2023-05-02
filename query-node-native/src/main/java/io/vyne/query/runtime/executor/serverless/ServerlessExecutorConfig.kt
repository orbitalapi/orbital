package io.vyne.query.runtime.executor.serverless

import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.function.adapter.aws.CustomRuntimeEventLoop
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@ConditionalOnProperty(value = ["vyne.consumer.serverless.enabled"], havingValue = "true", matchIfMissing = false)
class ServerlessExecutorConfig {

   companion object {
      private val logger = KotlinLogging.logger {}
   }
   init {
       logger.info { "Configuring instance to consumer serverless functions" }
   }
   @Bean
   @Profile("aws")
   fun awsEventLoop(context: ConfigurableApplicationContext): CustomRuntimeEventLoop {
      logger.info { "Custom AWS runtime event loop initializing" }
      return CustomRuntimeEventLoop(context)
   }


}
