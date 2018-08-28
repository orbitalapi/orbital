package io.vyne.tradeCompliance.aggregator

import io.polymer.spring.EnablePolymer
import io.polymer.spring.RemoteSchemaStoreType
import io.vyne.EnableVyneClient
import io.vyne.VyneClient
import io.vyne.tradeCompliance.RuleEvaluationResult
import io.vyne.tradeCompliance.TradeRequest
import io.vyne.tradeCompliance.TypeAliases
import lang.taxi.TypeAliasRegistry
import lang.taxi.annotations.Operation
import lang.taxi.annotations.Service
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.netflix.eureka.EnableEurekaClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController


@SpringBootApplication
@EnableEurekaClient
@EnablePolymer(remoteSchemaStore = RemoteSchemaStoreType.HAZELCAST)
@EnableVyneClient
class TradeComplianceEvaluatorApp {

   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         TypeAliasRegistry.register(TypeAliases::class, io.vyne.tradeCompliance.aggregator.TypeAliases::class)
         SpringApplication.run(TradeComplianceEvaluatorApp::class.java, *args)
      }
   }
}


