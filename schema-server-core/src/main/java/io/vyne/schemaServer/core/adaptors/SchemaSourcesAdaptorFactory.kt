package io.vyne.schemaServer.core.adaptors

class SchemaSourcesAdaptorFactory {

   fun getAdaptor(spec: PackageLoaderSpec): TaxiSchemaSourcesAdaptor {
      return when (spec.packageType) {
         PackageType.Taxi -> TaxiSchemaSourcesAdaptor()
         else -> TODO("Not Implemented: ${spec.packageType}")
      }
   }
}
