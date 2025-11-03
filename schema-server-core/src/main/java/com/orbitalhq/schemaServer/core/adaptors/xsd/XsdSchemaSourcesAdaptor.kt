package com.orbitalhq.schemaServer.core.adaptors.xsd

import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.asVersionedSource
import com.orbitalhq.schema.publisher.loaders.SourceGenerator
import com.orbitalhq.schemaServer.packages.OpenApiPackageLoaderSpec
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import lang.taxi.generators.SourceMap
import lang.taxi.packages.SourcesType
import lang.taxi.xsd.TaxiGenerator
import lang.taxi.xsd.XsdReaderConfig
import mu.KotlinLogging
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.toPath

class XsdSchemaSourceGenerator(private val configProvider: XsdSchemaSourceConfigProvider = EmptyXsdSourceConfigProvider) : SourceGenerator {
   companion object {
      const val XSD_SOURCES_TYPE = "@orbital/xsd"
      private val logger = KotlinLogging.logger {}
   }

   override fun supportsSources(sourcesType: SourcesType): Boolean {
      return sourcesType == XSD_SOURCES_TYPE
   }

   override fun generateSourcePackage(
      sourceFiles: List<VersionedSource>,
      packageMetadata: PackageMetadata
   ): SourcePackage {
      val taxiSource = sourceFiles.map { xsdSourceFile ->
         val config = configProvider.provide(xsdSourceFile, packageMetadata)
         logger.info { "Parsing XSD file in project ${packageMetadata.identifier} at ${xsdSourceFile.pathOrName}" }
         val generatedTaxiCode = try {
            TaxiGenerator(
               config = config
            ).generateAsStrings(xsdSourceFile.content.byteInputStream())
         } catch (e:Exception) {
            logger.error(e) { "Failed to parse XSD file in project ${packageMetadata.identifier} at ${xsdSourceFile.pathOrName} - ${e.message}" }
            throw e
         }

         generatedTaxiCode.sourceMap to generatedTaxiCode.asVersionedSource(
            packageMetadata.identifier,
            "GeneratedFrom_${xsdSourceFile.name}"
         )
      }
      val allTaxiSources = taxiSource.flatMap { it.second }

      val sourceMap = taxiSource.map { it.first }
         .reduceOrNull { acc, sourceMap -> acc.combine(sourceMap) }
         ?: SourceMap.EMPTY
      return SourcePackage.asTranspiledPackage(
         packageMetadata,
         sourceFiles,
         allTaxiSources,
         sourceMap
      )
   }
}

/**
 * Config which allows fine-tuning of how Xsd's are parsed.
 */
interface XsdSchemaSourceConfigProvider {
   fun provide(source: VersionedSource, packageMetadata: PackageMetadata): XsdReaderConfig
}

object EmptyXsdSourceConfigProvider : XsdSchemaSourceConfigProvider {
   override fun provide(source: VersionedSource, packageMetadata: PackageMetadata): XsdReaderConfig = XsdReaderConfig.EMPTY
}

class FileLoadingXsdSourceConfigProvider : XsdSchemaSourceConfigProvider {
   companion object {
      const val CONFIG_FILE_NAME = "xsd.taxi.conf"
      private val logger = KotlinLogging.logger {}
   }
   override fun provide(source: VersionedSource, packageMetadata: PackageMetadata): XsdReaderConfig {
      if (source.path == null) {
         logger.warn { "Cannot read an Xsd parser config file for source ${source.name} as no path was provided - using a default" }
         return XsdReaderConfig.EMPTY
      }
      val sourceFile = Paths.get(source.path)
      val uri = sourceFile.toUri()
      val isFile = (uri.scheme == "file" || uri.scheme == null)
      require(isFile) { "Reading Xsd parser configs in taxi projects is only supported on file-based URIs - found ${uri.toASCIIString()}" }
      val configFilePath = uri.toPath().parent.resolve(CONFIG_FILE_NAME)
      return if (Files.exists(configFilePath)) {
         readPackageLoaderSpec(configFilePath, packageMetadata)
      } else {
         XsdReaderConfig.EMPTY
      }
   }

   private fun readPackageLoaderSpec(configFilePath: Path, packageMetadata: PackageMetadata): XsdReaderConfig {
      val config = ConfigFactory.parseFile(configFilePath.toFile())
         .resolve()
      val readerConfig = config.extract<XsdReaderConfig>()
      return readerConfig.makeFilePathsRelativeTo(configFilePath.parent)
   }
}
