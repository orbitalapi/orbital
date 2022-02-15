package io.vyne.licensing

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.io.Resources
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableScheduling
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import kotlin.system.exitProcess

@Configuration
@EnableScheduling
class LicenseConfig {

   private val logger = KotlinLogging.logger {}

   private val defaultLicenseSearchPaths = listOf(
      Paths.get(System.getProperty("user.home"), ".vyne/license.json"),
      Paths.get("/opt/var/vyne/license/license.json"),
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
         fallbackLicenseDuration = Duration.ofMinutes(60)
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
               val licenseJson = licensePath.toFile().readText()
               val loadedLicense = Signing.objectMapper.readValue<License>(licenseJson)
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
         licenseValidator.fallbackLicense()
      } else {
         loadedLicense
      }
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
         logger.info { "License has expired.  Shutting down" }
         exitProcess(0)
      }
   }

   init {
      logger.info { "License monitor started.  This application will exit shortly after ${license.expiresOn}" }
      taskScheduler.schedule(exitTask, license.expiresOn)
   }
}
