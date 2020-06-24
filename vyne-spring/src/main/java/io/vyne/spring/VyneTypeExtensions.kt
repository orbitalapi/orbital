package io.vyne.spring

import io.vyne.schemas.Operation
import io.vyne.schemas.Service

fun Service.isServiceDiscoveryClient() = hasMetadata("ServiceDiscoveryClient")
fun Service.serviceDiscoveryClientName() = metadata("ServiceDiscoveryClient").params["serviceName"] as String
fun Operation.hasHttpMetadata(): Boolean {
   if (!this.hasMetadata("HttpOperation")) {
      return false;
   }
   val httpMeta = this.metadata("HttpOperation")
   return httpMeta.params.containsKey("url")
}

fun Operation.isHttpOperation() = metadata("HttpOperation").params["url"]?.let {
   val urlString = it as String
   urlString.startsWith("http://") || urlString.startsWith("https://")
} ?: false
