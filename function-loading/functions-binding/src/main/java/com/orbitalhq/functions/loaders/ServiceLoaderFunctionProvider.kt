package com.orbitalhq.functions.loaders

import com.orbitalhq.PackageMetadata
import com.orbitalhq.VersionedSource
import com.orbitalhq.functions.TaxiFunctionProvider
import mu.KotlinLogging
import java.net.URLClassLoader
import java.nio.file.Paths
import java.util.ServiceLoader

/**
 * Provides custom functions by loading JAR files using Java's ServiceLoader
 * approach.
 *
 * Tested in schema-server-core -> CustomFunctionJarLoadingTest
 */
class ServiceLoaderFunctionProvider : CustomFunctionProvider {
   companion object {
      private val logger = KotlinLogging.logger {}
   }
   override fun loadFunctionClasses(
       sourceFiles: List<VersionedSource>,
       packageMetadata: PackageMetadata
   ): List<TaxiFunctionProvider> {

      val jarURLS = sourceFiles.mapNotNull { sourceFile ->
         if (sourceFile.path == null) {
            logger.warn { "Can't load JAR from source file ${sourceFile.packageQualifiedName} as no path was provided" }
            return@mapNotNull null
         }
         val jarURI = Paths.get(sourceFile.path)
            .toAbsolutePath().toUri()
         logger.info { "Loading custom functions from ${jarURI.toASCIIString()}" }
         jarURI.toURL()
      }

      val classLoader = URLClassLoader(jarURLS.toTypedArray(), this::class.java.classLoader)
      val serviceLoader = ServiceLoader.load(TaxiFunctionProvider::class.java, classLoader)
      val providers = mutableListOf<TaxiFunctionProvider>()
      serviceLoader.forEach { provider ->
         logger.info { "Registering class ${provider::class.java.name} for scanning of custom functions" }
         providers.add(provider)
      }
      return providers
   }
}
