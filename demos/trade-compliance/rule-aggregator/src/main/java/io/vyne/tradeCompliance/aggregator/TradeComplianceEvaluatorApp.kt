package io.vyne.tradeCompliance.aggregator

import io.vyne.spring.EnableVyne
import io.vyne.spring.RemoteSchemaStoreType
import io.vyne.EnableVyneClient
import io.vyne.tradeCompliance.TypeAliases
import lang.taxi.TypeAliasRegistry
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.netflix.eureka.EnableEurekaClient
import org.springframework.cloud.netflix.feign.EnableFeignClients


@SpringBootApplication
@EnableEurekaClient
@EnableVyne(remoteSchemaStore = RemoteSchemaStoreType.HAZELCAST)
@EnableVyneClient
@EnableFeignClients
class TradeComplianceEvaluatorApp {

   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         TypeAliasRegistry.register(TypeAliases::class, io.vyne.tradeCompliance.aggregator.TypeAliases::class)
         SpringApplication.run(TradeComplianceEvaluatorApp::class.java, *args)
      }
   }
}


