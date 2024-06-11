package com.orbitalhq.licensing

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
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


   fun readAndValidateLicense(licensePath: Path): Either<InvalidLicenseException,License> {
      val licenseJson = licensePath.toFile().readText()
      return readAndValidateLicense(licenseJson)

   }
   fun readAndValidateLicense(licenseJson: String): Either<InvalidLicenseException,License> {
      val unvalidatedLicense: License = Signing.objectMapper.readValue(licenseJson)
      val (isValid,errorMessage) = validateLicense(unvalidatedLicense)
      return if (isValid) {
         unvalidatedLicense.right()
      } else {
         InvalidLicenseException(errorMessage).left()
      }
   }

   /**
    * Returns a fallback license, which allows the platform to run
    * if no license was found
    */
   fun fallbackLicense(licensee: String) = License.unlicensed(clock.instant().plus(fallbackLicenseDuration), licensee)

   private fun validateLicense(license: License): Pair<Boolean, String> {
      return when {
         !verifySignature(license) -> {
            logger.warn { "License failed verification." }
            false to "License failed verification"
         }

         license.expiresOn.isBefore(clock.instant()) -> {
            false to "License expired on ${license.expiresOn} which is before current time (${clock.instant()}."
         }

         else -> {
            true to "OK"
         }
      }
   }

   fun isValidLicense(license: License): Boolean {
      return validateLicense(license).first
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

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidLicenseException(message: String) : RuntimeException(message)
