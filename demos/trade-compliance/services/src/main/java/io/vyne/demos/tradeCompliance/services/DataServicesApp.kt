package io.vyne.demos.tradeCompliance.services

import io.polymer.spring.EnablePolymer
import io.polymer.spring.RemoteSchemaStoreType
import io.vyne.tradeCompliance.TypeAliases
import lang.taxi.TypeAliasRegistry
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.netflix.eureka.EnableEurekaClient


@SpringBootApplication
@EnableEurekaClient
@EnablePolymer(remoteSchemaStore = RemoteSchemaStoreType.HAZELCAST)
class DataServicesApp {
   companion object {

      @JvmStatic
      fun main(args: Array<String>) {
         TypeAliasRegistry.register(listOf(TypeAliases::class))
         SpringApplication.run(DataServicesApp::class.java, *args)
      }
   }
}
