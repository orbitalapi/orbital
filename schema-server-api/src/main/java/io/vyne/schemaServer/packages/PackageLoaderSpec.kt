package io.vyne.schemaServer.packages

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonSubTypes(
   JsonSubTypes.Type(OpenApiPackageLoaderSpec::class, name = "OpenApi"),
   JsonSubTypes.Type(TaxiPackageLoaderSpec::class, name = "Taxi")
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "packageType", include = JsonTypeInfo.As.EXISTING_PROPERTY)
interface PackageLoaderSpec {
   val packageType: PackageType
}

enum class PackageType {
   OpenApi,
   Taxi,
   Soap,
   Protobuf,
   JsonSchema
}

