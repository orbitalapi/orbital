package com.orbitalhq.licensing

import com.google.common.base.Throwables
import mu.KotlinLogging
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import reactor.core.publisher.Mono
import java.nio.file.Path
import kotlin.io.path.writeText

class OrbitalLicenseManager(
   private val licenseServerApi: OrbitalLicenseServerApi,
   private val licenseValidator: LicenseValidator,

   // Pass the file path (not a path to the directory containing the license)
   private val licenseFilePath: Path,
   initialLicense: License
) : LicenseManager(initialLicense) {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private fun saveLicense(licenseJson: String) {
      try {
         licenseFilePath.writeText(licenseJson)
         logger.info { "New license saved to ${licenseFilePath.toAbsolutePath()}" }
      } catch (e:Exception) {
         val rootCause = Throwables.getRootCause(e)
         throw RuntimeException("Attempt to save license to ${licenseFilePath.toAbsolutePath()} failed - ${rootCause::class.simpleName} - ${rootCause.message}.")
      }

   }

   /**
    * Downloads the license, using the authorization provided as a header.
    * If the authorization is inteded as a Bearer token, ensure that it's
    * passed with the Bearer prefix
    */
   fun downloadLicense(authorization: String): Mono<LicenseWithUsage> {
      logger.info { "Downloading license" }
      return licenseServerApi.getLicense(authorization)
         .flatMap { license ->
            logger.info { "Fetched new license - $license" }

            // Covert the license JSON to a Mono<License>, converting the error
            // to a Mono.error() if the license fails validation
            val validatedLicense = licenseValidator.readAndValidateLicense(license)
               .fold(
                  { throwable -> Mono.error(throwable) },
                  { value ->
                     saveLicense(license)
                     Mono.just(value)
                  }
               )
            validatedLicense.map { submitValidatedLicense(it) }
         }
   }
}


@HttpExchange
interface OrbitalLicenseServerApi {
   // Design choice: Return the model as a String, rather than as a License
   // even though it's a String of the JSON form of the License.
   // This is because we don't want to accept it as a "License" until it's
   // gone through our runtime, server-side validator
   // which is used to detect malicious changes to the license.
   // The validator accepts a String, rather than a License, so this
   // feels more natural
   @GetExchange("http://orbital-license-server/api/license")
   fun getLicense(
      @RequestHeader("Authorization") authorization: String
   ): Mono<String>

}
