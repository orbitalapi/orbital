package io.vyne.licensing

import mu.KotlinLogging
import java.nio.file.Path
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.time.Clock
import java.time.Duration
import java.util.Base64

class LicenseValidator(
   private val publicKey: PublicKey,
   private val fallbackLicenseDuration: Duration = Duration.ofHours(4L),
   private val clock: Clock = Clock.systemUTC()
) {
   private val logger = KotlinLogging.logger {}

   /**
    * Returns either the license provided, or a fallback
    * license, which expires after fallbackLicenseDuration.
    *
    * This allows users to run the system unlicensed for a short period of time.
    */
   fun validOrFallback(license: License): License {
      return if (isValidLicense(license)) {
         logger.info { "License obtained successfully: ${license.unsigned()}" }
         license
      } else {
         val fallback = fallbackLicense()
         logger.warn { "Issuing fallback license: $fallback" }
         fallback
      }
   }

   fun fallbackLicense() = License.unlicensed(clock.instant().plus(fallbackLicenseDuration))

   fun isValidLicense(license: License): Boolean {
      return when {
         !verifySignature(license) -> {
            logger.warn { "License failed verification." }
            false
         }
         license.expiresOn.isBefore(clock.instant()) -> {
            logger.warn { "License expired on ${license.expiresOn} which is before current time (${clock.instant()}." }
            false
         }
         else -> {
            true
         }
      }
   }


   fun verifySignature(license: License): Boolean {
      require(license.signature != null) { "Cannot verify license, as license is not signed!" }
      val unsigned = license.unsigned()
      val signer = Signing.signer
      signer.initVerify(publicKey)
      signer.update(unsigned.verificationClaim())
      return try {
         val signatureBytes = Base64.getDecoder().decode(license.signature)
         signer.verify(signatureBytes)
      } catch (e: Exception) {
         logger.error(e) { "Failed to verify license: ${e.message}" }
         false
      }


   }

   companion object {
      fun forPublicKeyAtPath(
         path: Path,
         fallbackLicenseDuration: Duration = Duration.ofHours(4L),
         clock: Clock = Clock.systemUTC()
      ): LicenseValidator {
         val bytes = path.toFile().readBytes()
         return forPublicKey(bytes, fallbackLicenseDuration, clock)
      }

      fun forPublicKey(
         bytes: ByteArray, fallbackLicenseDuration: Duration = Duration.ofHours(1L),
         clock: Clock = Clock.systemUTC()
      ): LicenseValidator {
         val publicKey = Signing.keyFactory
            .generatePublic(X509EncodedKeySpec(bytes))
         return LicenseValidator(publicKey, fallbackLicenseDuration, clock)
      }
   }
}
