package com.orbitalhq.schemaServer.core.adaptors

import com.orbitalhq.schema.publisher.loaders.SchemaSourcesAdaptor
import com.orbitalhq.schemaServer.core.adaptors.openapi.OpenApiSchemaSourcesAdaptor
import com.orbitalhq.schemaServer.core.adaptors.soap.SoapSchemaSourcesAdaptor
import com.orbitalhq.schemaServer.core.adaptors.taxi.TaxiSchemaSourcesAdaptor
import com.orbitalhq.schemaServer.packages.OpenApiPackageLoaderSpec
import com.orbitalhq.schemaServer.packages.PackageLoaderSpec
import com.orbitalhq.schemaServer.packages.PackageType
import com.orbitalhq.schemaServer.packages.SoapPackageLoaderSpec

class SchemaSourcesAdaptorFactory {

   fun getAdaptor(spec: PackageLoaderSpec): SchemaSourcesAdaptor {
      return when (spec.packageType) {
         PackageType.Taxi -> TaxiSchemaSourcesAdaptor()
         PackageType.OpenApi -> OpenApiSchemaSourcesAdaptor(spec as OpenApiPackageLoaderSpec)
         PackageType.Soap -> SoapSchemaSourcesAdaptor(spec as SoapPackageLoaderSpec)
         else -> TODO("Not Implemented: ${spec.packageType}")
      }
   }
}
