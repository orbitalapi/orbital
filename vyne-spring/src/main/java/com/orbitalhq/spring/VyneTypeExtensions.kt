package com.orbitalhq.spring

import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import lang.taxi.annotations.HttpOperation

fun Service.isServiceDiscoveryClient() = hasMetadata("ServiceDiscoveryClient")
fun RemoteOperation.hasHttpMetadata(): Boolean {
   val annotationName = HttpOperation.NAME
   if (!this.hasMetadata(annotationName)) {
      return false;
   }
   val httpMeta = this.metadata(annotationName)
   return httpMeta.params.containsKey("url")
}
