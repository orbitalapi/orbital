package io.polymer.spring.invokers

import io.osmosis.polymer.schemas.Operation
import io.osmosis.polymer.schemas.Service

interface ServiceUrlResolver {
   fun canResolve(service: Service, operation: Operation):Boolean
   fun makeAbsolute(url:String, service:Service,operation: Operation):String
}

class ServiceDiscoveryClientUrlResolver : ServiceUrlResolver {
   override fun canResolve(service: Service, operation: Operation): Boolean = service.hasMetadata("ServiceDiscoveryClient")

   override fun makeAbsolute(url: String, service: Service, operation: Operation): String {
      val serviceName = service.metadata("ServiceDiscoveryClient").params["serviceName"]
      return "http://$serviceName/${url.trimStart('/')}"
   }

}

