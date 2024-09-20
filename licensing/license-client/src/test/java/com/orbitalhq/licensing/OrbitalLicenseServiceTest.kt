package com.orbitalhq.licensing

import arrow.core.left
import arrow.core.right
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.io.TempDir
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import reactor.core.publisher.Mono
import java.io.File
import java.time.Instant

class OrbitalLicenseServiceTest {
   lateinit var licenseService: OrbitalLicenseService
   lateinit var validator: LicenseValidator
   lateinit var licenseManager: OrbitalLicenseManager
   lateinit var licenseServerApi: OrbitalLicenseServerApi

   @TempDir
   lateinit var tempDir: File

   @BeforeEach
   fun setup() {
      this.validator = mock { }
      this.licenseServerApi = mock { }
      licenseManager = OrbitalLicenseManager(
         licenseServerApi,
         validator,
         tempDir.resolve("license.json").toPath(),
         License.unlicensed(Instant.now().plusSeconds(3600), "Test Server"),
      )
      licenseService = OrbitalLicenseService(
         licenseManager,
         validator
      )
   }

   @Test
   fun `submitting a valid license updates the license`(): Unit = runBlocking {
      val validLicense = validLicense()
      whenever(validator.readAndValidateLicense(any<String>())).thenReturn(validLicense.right())
      val response = licenseService.submitLicense("{ this isn't checked }")
      response.license.shouldBe(validLicense)
      response.usage.shouldHaveSize(1)
   }

   @Test
   fun `submitting an invalid license throws an error`(): Unit = runBlocking {
      whenever(validator.readAndValidateLicense(any<String>())).thenReturn(InvalidLicenseException("Validation failed").left())
      val exception = assertThrows<InvalidLicenseException> {
         licenseService.submitLicense("{ this isn't checked }")
      }
   }

   @Test
   fun `can fetch license`(): Unit = runBlocking {
      val validLicense = validLicense()
      whenever(validator.readAndValidateLicense(any<String>())).thenReturn(validLicense.right())
      licenseService.submitLicense("{ this isn't checked }")

      val currentLicense = licenseService.getLicenseStatus(Mono.just(mockJwt())).block()!!
      currentLicense.license.shouldBe(validLicense)
      currentLicense.usage.shouldHaveSize(1)
   }

   @Test
   fun `if license is fallback license then a license is downloaded`() {
      // Current license is a fallback license
      licenseManager.submitValidatedLicense(
         License.unlicensed(Instant.now().plusSeconds(3600), "Test Server"),
      )

      whenever(validator.readAndValidateLicense(any<String>())).thenReturn(validLicense().right())
      whenever(licenseServerApi.getLicense(any())).thenReturn(Mono.just("{ this isn't checked }"))
      val authToken = mockJwt()
      licenseService.getLicenseStatus(Mono.just(authToken))
         .block()

      verify(licenseServerApi).getLicense(any())

   }

   private fun mockJwt(): JwtAuthenticationToken {
      val mockTokenValue = "abcdefg"
      val mockOauthToken = mock<Jwt> {
         on { tokenValue } doReturn mockTokenValue
      }
      val jwtAuthenticationToken = mock<JwtAuthenticationToken> {
         on { token } doReturn mockOauthToken
      }
      return jwtAuthenticationToken
   }

   @Test
   fun `if license is valid then a new license is not downloaded`() {
      // Current license is a real license
      licenseManager.submitValidatedLicense(validLicense())
      val authToken = mockJwt()
      licenseService.getLicenseStatus(Mono.just(authToken))
         .block()

      verify(licenseServerApi, never()).getLicense(any())
   }

}

fun validLicense(): License {
   return License(
      "Jimmy Valid",
      Instant.now().plusSeconds(3600),
      Instant.now(),
      "Enterprise",
      emptyList(),
      listOf(
         UsageQuota(
            MeteredCapability.User, 100
         )
      )
   )
}
