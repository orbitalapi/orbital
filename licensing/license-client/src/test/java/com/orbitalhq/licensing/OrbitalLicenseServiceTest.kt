package com.orbitalhq.licensing

import arrow.core.left
import arrow.core.right
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class OrbitalLicenseServiceTest {
   lateinit var licenseService: OrbitalLicenseService
   lateinit var validator: LicenseValidator

   @BeforeEach
   fun setup() {
      this.validator = mock { }
      licenseService = OrbitalLicenseService(
         LicenseManager(
            License.unlicensed(Instant.now().plusSeconds(3600), "Test Server"),
         ),
         validator
      )
   }

   @Test
   fun `submitting a valid license updates the license`() {
      val validLicense = validLicense()
      whenever(validator.readAndValidateLicense(any<String>())).thenReturn(validLicense.right())
      val response = licenseService.submitLicense("{ this isn't checked }")
      response.license.shouldBe(validLicense)
      response.usage.shouldHaveSize(1)
   }

   @Test
   fun `submitting an invalid license throws an error`() {
      whenever(validator.readAndValidateLicense(any<String>())).thenReturn(InvalidLicenseException("Validation failed").left())
      val exception = assertThrows<InvalidLicenseException> {
         licenseService.submitLicense("{ this isn't checked }")
      }
   }

   @Test
   fun `can fetch license`() {
      val validLicense = validLicense()
      whenever(validator.readAndValidateLicense(any<String>())).thenReturn(validLicense.right())
      licenseService.submitLicense("{ this isn't checked }")

      val currentLicense = licenseService.getLicenseStatus()
      currentLicense.license.shouldBe(validLicense)
      currentLicense.usage.shouldHaveSize(1)
   }

   private fun validLicense(): License {
      return License(
         "Jimmy Valid",
         Instant.now().plusSeconds(3600),
         Instant.now(),
         LicensedEdition.ENTERPRISE,
         listOf(
            UsageQuota(
               MeteredCapability.User, 100
            )
         )
      )
   }
}
