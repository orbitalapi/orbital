package com.orbitalhq.schemaServer.core.repositories.upload

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schemaServer.packages.PackageType
import com.orbitalhq.schemaServer.repositories.AddFileProjectRequest
import com.orbitalhq.spring.http.BadRequestException
import java.nio.file.Path
import kotlin.reflect.KProperty1

interface ProjectUploadHandler {
   val packageType: PackageType
   fun processUpload(
      packageId: PackageIdentifier,
      requestParams: Map<String, List<String>>,
      payload: ByteArray,
      projectRoot: Path
   ): AddFileProjectRequest

   companion object {
      val DEFAULT = listOf(AvroProjectUploadHandler(), OpenApiProjectUploadHandler(), TaxiProjectUploadHandler())
   }
}


/**
 * Extension of ProjectUploadHandler for cases where the packageId is determined by
 * reading the upload payload, rather than provided in a request parameter
 */
interface PackageIdentifyingUploadHandler : ProjectUploadHandler {
   fun readPackageIdentifier(payload: ByteArray):PackageIdentifier
}

fun Map<String,List<String>>.singleOrNullOrBadRequest(property: KProperty1<*,*>): String? {
   return singleOrNullOrBadRequest(property.name)
}
fun Map<String,List<String>>.singleOrNullOrBadRequest(name: String): String? {
   if (!this.containsKey(name)) return null
   val value = this[name]!!
   return when {
      value.isEmpty() -> return null
      value.size == 1 -> value[0]
      else -> throw BadRequestException("Expected exactly one parameter $name, but got ${value.size}")
   }
}
fun Map<String,List<String>>.singleOrBadRequest(name: String): String {
   return singleOrNullOrBadRequest(name) ?: throw BadRequestException("Expected parameter $name")
}

fun Map<String,List<String>>.singleOrBadRequest(property: KProperty1<*,*>): String {
   return singleOrBadRequest(property.name)
}
