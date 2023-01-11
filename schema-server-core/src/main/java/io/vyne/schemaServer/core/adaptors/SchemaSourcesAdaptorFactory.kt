package io.vyne.schemaServer.core.adaptors

import io.vyne.schema.publisher.loaders.SchemaSourcesAdaptor
import io.vyne.schemaServer.core.adaptors.openapi.OpenApiSchemaSourcesAdaptor
import io.vyne.schemaServer.core.adaptors.taxi.TaxiSchemaSourcesAdaptor

class SchemaSourcesAdaptorFactory {

   fun getAdaptor(spec: PackageLoaderSpec): SchemaSourcesAdaptor {
      return when (spec.packageType) {
         PackageType.Taxi -> TaxiSchemaSourcesAdaptor()
         PackageType.OpenApi -> OpenApiSchemaSourcesAdaptor(spec as OpenApiPackageLoaderSpec)
         else -> TODO("Not Implemented: ${spec.packageType}")
      }
   }
}
