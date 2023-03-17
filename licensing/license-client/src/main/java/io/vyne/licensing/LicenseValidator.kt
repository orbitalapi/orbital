package io.vyne.licensing

import mu.KotlinLogging
import java.nio.file.Path
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.time.Clock
import java.time.Duration
import java.util.*

class LicenseValidator(
   private val publicKey: PublicKey,
   private val fallbackLicenseDuration: Duration = defaultFallbackLicenseDuration,
   private val clock: Clock = Clock.systemUTC()
) {
   private val logger = KotlinLogging.logger {}


   /**
    * Returns a fallback license, which allows the platform to run
    * if no license was found
    */
   fun fallbackLicense(licensee: String) = License.unlicensed(clock.instant().plus(fallbackLicenseDuration), licensee)

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
      val defaultFallbackLicenseDuration = Duration.ofDays(999)
      fun forPublicKeyAtPath(
         path: Path,
         fallbackLicenseDuration: Duration = defaultFallbackLicenseDuration,
         clock: Clock = Clock.systemUTC()
      ): LicenseValidator {
         val bytes = path.toFile().readBytes()
         return forPublicKey(bytes, fallbackLicenseDuration, clock)
      }

      fun forPublicKey(
         bytes: ByteArray,
         fallbackLicenseDuration: Duration = defaultFallbackLicenseDuration,
         clock: Clock = Clock.systemUTC()
      ): LicenseValidator {
         val publicKey = Signing.keyFactory
            .generatePublic(X509EncodedKeySpec(bytes))
         return LicenseValidator(publicKey, fallbackLicenseDuration, clock)
      }
   }
}
