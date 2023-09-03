package com.orbitalhq.licensing

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.io.Resources
import com.orbitalhq.utils.Ids
import com.orbitalhq.utils.Names
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableScheduling
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess

@Configuration
@EnableScheduling
class LicenseConfig {

   private val logger = KotlinLogging.logger {}

   private val fallbackLicensePath = Paths.get(".", "/fallback-license.json")
   private val defaultLicenseSearchPaths = listOf(
      Paths.get(System.getProperty("user.home"), ".orbital/license.json"),
      Paths.get(System.getProperty("user.home"), ".vyne/license.json"),
      Paths.get("/opt/var/orbital/license/license.json"),
   )

   /**
    * Loads a license, searching from default paths, and optionally including a user
    * specified path.
    * The license is validated against the public key.
    * If license validation fails, a fallback license is issued, which expires shortly.
    */
   @Bean
   fun license(@Value("\${vyne.license.path:#{null}}") licensePath: Path?): License {
      val publicKey = Resources.toByteArray(Resources.getResource("vyne-license-pub.der"))
      val validator = LicenseValidator.forPublicKey(
         publicKey,
         fallbackLicenseDuration = LicenseValidator.defaultFallbackLicenseDuration
      )
      val pathsToSearch = if (licensePath != null) {
         listOf(licensePath) + defaultLicenseSearchPaths
      } else {
         logger.info { "No license location found - will look in the default locations.  Modify this by passing --vyne.license.path on startup" }
         defaultLicenseSearchPaths
      }
      val license = loadLicenseJson(pathsToSearch, validator)
      logger.info { license.toString() }
      return license
   }

   private fun loadLicenseJson(pathsToSearch: List<Path>, licenseValidator: LicenseValidator): License {
      logger.info { "Looking for license file" }
      val loadedLicense = pathsToSearch
         .asSequence()
         .filter { path ->
            if (!Files.exists(path)) {
               logger.info { "No license found at ${path.toAbsolutePath()}" }
               false
            } else {
               true
            }
         }.mapNotNull { licensePath ->
            try {
               logger.info { "Attempting to load license from ${licensePath.toAbsolutePath()}" }
               val loadedLicense = loadUnvalidatedLicense(licensePath)
               if (licenseValidator.isValidLicense(loadedLicense)) {
                  logger.info { "Successfully loaded license at ${licensePath.toAbsolutePath()}" }
                  loadedLicense
               } else {
                  logger.warn { "License found at ${licensePath.toAbsolutePath()} is not valid.  Will continue looking" }
                  null
               }
            } catch (e: Exception) {
               logger.warn { "Failed to load license from ${licensePath.toAbsolutePath()} - ${e.message}.  Will continue looking" }
               null
            }
         }.firstOrNull()

      return if (loadedLicense == null) {
         logger.warn { "No license found.  Will use a fallback license." }
         getOrCreateFallbackLicense(licenseValidator)
      } else {
         loadedLicense
      }
   }

   private fun getOrCreateFallbackLicense(licenseValidator: LicenseValidator): License {
      return if (Files.exists(fallbackLicensePath)) {
         logger.info { "Using existing fallback license at ${fallbackLicensePath.toFile().canonicalPath}" }
         loadUnvalidatedLicense(fallbackLicensePath).copy(isFallbackLicense = true)
      } else {
         val name = Names.randomName(suffix = Ids.id("", 4))
         val license = licenseValidator.fallbackLicense(name)
         logger.info { "Creating new fallback license at $fallbackLicensePath" }
         Signing.objectMapper.writerWithDefaultPrettyPrinter().writeValue(fallbackLicensePath.toFile(), license)
         logger.info { "Fallback license for $name created at ${fallbackLicensePath.toFile().canonicalPath}" }
         license
      }
   }

   private fun loadUnvalidatedLicense(licensePath: Path): License {
      val licenseJson = licensePath.toFile().readText()
      return Signing.objectMapper.readValue(licenseJson)
   }

   @Bean
   fun licenseMonitor(
      taskScheduler: TaskScheduler,
      license: License
   ): LicenseMonitor {
      return LicenseMonitor(taskScheduler, license)
   }

}


class LicenseMonitor(
   private val taskScheduler: TaskScheduler,
   private val license: License,
   private val exitTask: () -> Unit = LicenseMonitor.Companion::logAndExit
) {
   companion object {
      private val logger = KotlinLogging.logger {}
      fun logAndExit() {
         logger.info { "************************************" }
         logger.info { "************************************" }
         logger.info { "************************************" }
         logger.info { "License has expired.  Shutting down" }
         logger.info { "************************************" }
         logger.info { "************************************" }
         logger.info { "************************************" }
         exitProcess(0)
      }
   }

   init {
      logger.info { "License monitor started.  This application will exit shortly after ${license.expiresOn}" }
      taskScheduler.schedule(exitTask, license.expiresOn)
   }
}