package io.vyne.tradeCompliance.rules

import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.polymer.spring.EnablePolymer
import io.polymer.spring.RemoteSchemaStoreType
import io.vyne.tradeCompliance.TypeAliases
import lang.taxi.TypeAliasRegistry
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.netflix.eureka.EnableEurekaClient
import org.springframework.context.annotation.Bean

@SpringBootApplication
@EnableEurekaClient
@EnablePolymer(remoteSchemaStore = RemoteSchemaStoreType.HAZELCAST)
class RulesProviderApp {
   companion object {

      @JvmStatic
      fun main(args: Array<String>) {
         TypeAliasRegistry.register(listOf(TypeAliases::class))
         SpringApplication.run(RulesProviderApp::class.java, *args)
      }
   }

   @Bean
   fun kotlinModule():KotlinModule {
      return KotlinModule();
   }
}
