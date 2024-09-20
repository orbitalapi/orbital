package com.orbitalhq.licensing

import arrow.core.left
import arrow.core.right
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.file.shouldNotExist
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import reactor.core.publisher.Mono
import reactor.kotlin.test.test
import java.io.File
import java.time.Instant

class OrbitalLicenseManagerTest {

   @TempDir
   lateinit var testDir: File

   lateinit var licenseServerApi: OrbitalLicenseServerApi
   lateinit var licenseManager: OrbitalLicenseManager
   lateinit var licenseValidator: LicenseValidator

   @BeforeEach
   fun setup() {
      licenseValidator = mock { }
      licenseServerApi = mock {  }
      licenseManager = OrbitalLicenseManager(
         licenseServerApi,
         licenseValidator,
         testDir.resolve("license.json").toPath(),
         License.unlicensed(Instant.MAX, "Someone")
      )
   }

   @Test
   fun `downloads and saves license`() {
      val licenseString = "{ license }"
      whenever(licenseServerApi.getLicense(any())).thenReturn(Mono.just(licenseString))
      val validLicense = validLicense()
      whenever(licenseValidator.readAndValidateLicense(any<String>())).thenReturn(validLicense.right())
      val downloadedLicense = licenseManager.downloadLicense("Bearer authToken")
         .block()!!

      downloadedLicense.license shouldBe validLicense

      verify(licenseServerApi).getLicense("Bearer authToken")
      testDir.resolve("license.json").shouldExist()
      testDir.resolve("license.json").readText().shouldBe(licenseString)

   }

   @Test
   fun `when valid license is downloaded then the license manager updates the license`() {
      val licenseString = "{ license }"
      whenever(licenseServerApi.getLicense(any())).thenReturn(Mono.just(licenseString))
      val validLicense = validLicense()
      whenever(licenseValidator.readAndValidateLicense(any<String>())).thenReturn(validLicense.right())

      licenseManager.licenseUpdated
         .test()
         .expectSubscription()
         .then {
            licenseManager.downloadLicense("Bearer authToken")
               .block()!!
         }
         .expectNextMatches { updatedLicense ->
            updatedLicense.license.shouldBe(validLicense)
            // also verify that the license returned is our new license
            licenseManager.license.shouldBe(validLicense)
            true
         }
         .thenCancel()
         .verify()
   }


   @Test
   fun `rejects license if the server returns an incorrectly signed license`() {
      // The license file doesn't exist
      testDir.resolve("license.json").shouldNotExist()

      val licenseString = "{ license }"
      whenever(licenseServerApi.getLicense(any())).thenReturn(Mono.just(licenseString))
      val invalidLicenseException = InvalidLicenseException("License is invalid")
      whenever(licenseValidator.readAndValidateLicense(any<String>())).thenReturn(invalidLicenseException.left())

      val downloadedLicense = licenseManager.downloadLicense("Bearer authToken")

      downloadedLicense.test()
         .expectErrorMatches { it == invalidLicenseException }
         .verify()

      // The license file still doesn't exist
      testDir.resolve("license.json").shouldNotExist()
   }
}
