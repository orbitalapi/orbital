package com.orbitalhq.licensing

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.io.Resources
import com.winterbe.expekt.should
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import org.junit.Test
import java.time.Instant
import kotlin.io.path.toPath

/**
 * NOTE: This test validates licenses that were created with the external license-vendor app.
 * To regenerate, they need to be regenerated using that app.
 * See ValidationGenerationTest
 */
class LicenseValidatorTest {

   val loader = LicenseValidator.forPublicKeyAtPath(Resources.getResource("test-license-key_pub.der").toURI().toPath())

   val license = License(
      "Jimmy",
      Instant.parse("2030-02-14T15:30:00Z"),
      Instant.parse("2019-02-14T15:30:00Z"),
      LicensedEdition.ENTERPRISE,
      quotas = emptyList()
   )

   @Test
   fun `can generate and load a license with a valid keypair`() {
      val validLicenseJson = Resources.getResource("test-licenses/valid-license.json")
         .readText()
      val licenseFromJson = Signing.objectMapper.readValue<License>(validLicenseJson)
      loader.verifySignature(licenseFromJson)
         .shouldBeTrue()
   }

   @Test
   fun `license signed with wrong keypair fails verification`() {
      val licenseJson = Resources.getResource("test-licenses/license-with-wrong-keypair.json")
         .readText()
      val licenseFromJson = Signing.objectMapper.readValue<License>(licenseJson)
      loader.verifySignature(licenseFromJson)
         .shouldBeFalse()
   }

   @Test
   fun `license with invalid base64 characters in signature fails verification`() {
      val validLicenseJson = Resources.getResource("test-licenses/tampered-license-invalid-chars-in-signature.json")
         .readText()
      val licenseFromJson = Signing.objectMapper.readValue<License>(validLicenseJson)
      loader.verifySignature(licenseFromJson)
         .shouldBeFalse()
   }

   @Test
   fun `license with invalid signature length fails verification`() {
      val validLicenseJson = Resources.getResource("test-licenses/tampered-license-invalid-signature.json")
         .readText()
      val licenseFromJson = Signing.objectMapper.readValue<License>(validLicenseJson)
      loader.verifySignature(licenseFromJson)
         .shouldBeFalse()
   }

   @Test
   fun `tampered license fails verification`() {
      val validLicenseJson = Resources.getResource("test-licenses/tampered-license.json")
         .readText()
      val licenseFromJson = Signing.objectMapper.readValue<License>(validLicenseJson)
      loader.verifySignature(licenseFromJson)
         .shouldBeFalse()
   }

   @Test
   fun `expired license is not valid`() {
      val validLicenseJson = Resources.getResource("test-licenses/expired-license.json")
         .readText()
      val licenseFromJson = Signing.objectMapper.readValue<License>(validLicenseJson)
      loader.isValidLicense(licenseFromJson)
         .shouldBeFalse()
   }



   @Test
   fun `license gives reasonable toString()`() {
      val string = license.toString()
      string.should.startWith("Licensed to Jimmy with Enterprise edition.  Expires on Thursday, 14 February 2030")
   }

}
