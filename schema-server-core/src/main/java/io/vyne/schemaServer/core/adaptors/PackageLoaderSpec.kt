package io.vyne.schemaServer.core.adaptors

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.typesafe.config.Config
import io.github.config4k.ClassContainer
import io.github.config4k.CustomType
import io.github.config4k.extract
import io.github.config4k.toConfig
import io.vyne.schemaServer.core.adaptors.taxi.TaxiPackageLoaderSpec
import java.net.URI
import java.time.Instant
import kotlin.reflect.full.isSubclassOf

interface PackageLoaderSpec {
   val packageType: PackageType
}

enum class PackageType {
   OpenApi,
   Taxi,
   Protobuf,
   JsonSchema
}

object InstantHoconSupport : CustomType {
   override fun parse(clazz: ClassContainer, config: Config, name: String): Any? {
      val instantStr = config.getString(name)
      return Instant.parse(instantStr)
   }

   override fun testParse(clazz: ClassContainer): Boolean {
      return clazz.mapperClass == Instant::class
   }

   override fun testToConfig(obj: Any): Boolean {
      return obj is Instant
   }

   override fun toConfig(obj: Any, name: String): Config {
      return (obj as Instant).toString().toConfig(name)
   }

}

object UriHoconSupport : CustomType {
   override fun parse(clazz: ClassContainer, config: Config, name: String): Any? {
      val uriString = config.getString(name)
      return URI.create(uriString)
   }

   override fun testParse(clazz: ClassContainer): Boolean {
      return clazz.mapperClass == URI::class
   }

   override fun testToConfig(obj: Any): Boolean {
      return obj is URI
   }

   override fun toConfig(obj: Any, name: String): Config {
      return (obj as URI).toString().toConfig(name)
   }

}

object PackageLoaderSpecHoconSupport : CustomType {
   private val objectMapper = jacksonObjectMapper()
      .findAndRegisterModules()
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

   override fun parse(clazz: ClassContainer, config: Config, name: String): Any? {
      val loaderConfig = config.getConfig(name)
      val declaredPackageType = loaderConfig.getString(PackageLoaderSpec::packageType.name)
      val packageType = PackageType.valueOf(declaredPackageType)
      return when (packageType) {
         PackageType.OpenApi -> loaderConfig.extract<OpenApiPackageLoaderSpec>()
         PackageType.Taxi -> TaxiPackageLoaderSpec
         else -> TODO("Not yet implemented: $packageType")
      }

   }

   override fun testParse(clazz: ClassContainer): Boolean {
      return clazz.mapperClass.isSubclassOf(PackageLoaderSpec::class)
   }

   override fun testToConfig(obj: Any): Boolean {
      return obj is PackageLoaderSpec
   }

   override fun toConfig(obj: Any, name: String): Config {
      val config = objectMapper.convertValue<Map<String, Any>>(obj).toConfig(name)
      return config
   }

}
