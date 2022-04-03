package io.vyne.licensing

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.io.Resources
import com.winterbe.expekt.should
import io.vyne.licensing.vendor.LicenseVendor
import io.vyne.utils.ManualClock
import org.junit.Test
import java.time.Duration
import java.time.Instant
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.toPath

@OptIn(ExperimentalPathApi::class)
class LicenseValidatorTest {

   val vendor = LicenseVendor.forPrivateKeyAtPath(
      Resources.getResource("test-license-key.der").toURI().toPath()
   )
   val loader = LicenseValidator.forPublicKeyAtPath(Resources.getResource("test-license-key_pub.der").toURI().toPath())

   val license = License(
      "Jimmy",
      Instant.parse("2030-02-14T15:30:00Z"),
      Instant.parse("2019-02-14T15:30:00Z"),
      LicensedEdition.ENTERPRISE
   )

   @Test
   fun `can generate and load a license with a valid keypair`() {
      val signedLicense = vendor.generateSignedLicense(license)
      val licenseJson = Signing.objectMapper.writerWithDefaultPrettyPrinter()
         .writeValueAsString(signedLicense)

      val licenseFromJson = Signing.objectMapper.readValue<License>(licenseJson)
      loader.verifySignature(licenseFromJson)
         .should.be.`true`
   }

   @Test
   fun `license with invalid license fails verification`() {
      val invalidVendor = LicenseVendor.forPrivateKeyAtPath(
         Resources.getResource("invalid-license.der").toURI().toPath()
      )
      val signedLicense = invalidVendor.generateSignedLicense(license)
      loader.verifySignature(signedLicense).should.be.`false`
   }

   @Test
   fun `license with invalid base64 characters in signature fails verification`() {
      val invalidLicense = license.copy(signature = "Naughty naughty") // spaces aren't permitted
      loader.verifySignature(invalidLicense)
         .should.be.`false`
   }

   @Test
   fun `license with invalid signature length fails verification`() {
      val invalidLicense = license.copy(signature = "NaughtyNaughty") // causes a signature legnth exception
      loader.verifySignature(invalidLicense)
         .should.be.`false`
   }

   @Test
   fun `tampered license fails verification`() {
      val signedLicense = vendor.generateSignedLicense(license)
      val tamperedLicense = signedLicense.copy(edition = LicensedEdition.STARTER)
      val licenseJson = Signing.objectMapper.writerWithDefaultPrettyPrinter()
         .writeValueAsString(tamperedLicense)

      val licenseFromJson = Signing.objectMapper.readValue<License>(licenseJson)
      loader.verifySignature(licenseFromJson)
         .should.be.`false`
   }

   @Test
   fun `expired license is not valid`() {
      val expiredLicense = license.copy(expiresOn = Instant.now().minus(Duration.ofMinutes(1L)))
      val signedExpiredLicense = vendor.generateSignedLicense(expiredLicense)

      loader.isValidLicense(signedExpiredLicense).should.be.`false`
   }

   @Test
   fun `loader returns provided license when valid`() {
      val signedLicense = vendor.generateSignedLicense(license)
      loader.validOrFallback(signedLicense).should.equal(signedLicense)
   }

   @Test
   fun `loader returns fallback license when invalid license provided`() {
      val clock = ManualClock(Instant.now())
      val loader = LicenseValidator.forPublicKeyAtPath(
         Resources.getResource("test-license-key_pub.der").toURI().toPath(),
         fallbackLicenseDuration = Duration.ofMinutes(10L),
         clock = clock
      )

      val signedLicense = vendor.generateSignedLicense(license)
      val tamperedLicense = signedLicense.copy(edition = LicensedEdition.STARTER)
      val loadedLicense = loader.validOrFallback(tamperedLicense)

      loadedLicense.isFallbackLicense.should.be.`true`
      loadedLicense.expiresOn.should.equal(clock.instant().plus(Duration.ofMinutes(10L)))
   }

   @Test
   fun `license gives reasonable toString()`() {
      val string = license.toString()
      string.should.startWith("Licensed to Jimmy with Enterprise edition.  Expires on Thursday, 14 February 2030")
   }

}
