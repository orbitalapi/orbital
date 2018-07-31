package io.vyne.tradeCompliance.aggregator

import io.osmosis.polymer.models.json.addAnnotatedInstance
import io.polymer.spring.EnablePolymer
import io.polymer.spring.PolymerFactory
import io.polymer.spring.RemoteSchemaStoreType
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
class TradeComplianceEvaluatorApp {

   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         TypeAliasRegistry.register(TypeAliases::class, io.vyne.tradeCompliance.aggregator.TypeAliases::class)
         SpringApplication.run(TradeComplianceEvaluatorApp::class.java, *args)
      }
   }
}


@RestController
@Service
class TradeComplianceEvaluator(
   private val vyneFactory: PolymerFactory
) {

   @PostMapping("/tradeCompliance")
   @Operation
   fun evaluate(trade: TradeRequest): TradeComplianceResult {
      val vyne = vyneFactory.createPolymer().query()
      vyne.addAnnotatedInstance(trade)
      val result = vyne.find("RuleEvaluationResults")

      val ruleEvaluations = result["RuleEvaluationResults"]!!.value as RuleEvaluationResults
      return TradeComplianceResult(ruleEvaluations)
   }
}
