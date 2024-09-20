package com.orbitalhq.licensing

import arrow.core.getOrElse
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.nimbusds.oauth2.sdk.auth.JWTAuthentication
import com.orbitalhq.security.VynePrivileges
import com.orbitalhq.spring.config.OrbitalOnly
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import java.security.Principal

@RestController
@OrbitalOnly
class OrbitalLicenseService(
   private val licenseManager: OrbitalLicenseManager,
   private val validator: LicenseValidator
) : BaseLicenseStatusService(licenseManager) {

   private val licenseUpdatedSink = Sinks.many().multicast().directAllOrNothing<LicenseWithUsage>()
   val licenseUpdated = licenseUpdatedSink.asFlux()

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   @PostMapping("/api/license")
   @PreAuthorize("hasAuthority('${VynePrivileges.ModifyLicense}')")
   suspend fun submitLicense(@RequestBody licenseJson: String): LicenseWithUsage {
      val validationResult = validator.readAndValidateLicense(licenseJson)
      val validatedLicense = validationResult.getOrElse { exception -> throw exception }
      return licenseManager.submitValidatedLicense(validatedLicense)
   }

   @GetMapping("/api/license/status", params = ["refresh"])
   @PreAuthorize("hasAuthority('${VynePrivileges.ModifyLicense}')")
   open fun refreshLicenseStatus(@AuthenticationPrincipal principal: Mono<Authentication>? = null, @RequestParam("refresh") refresh: Boolean): Mono<LicenseWithUsage> {
      return getLicenseStatus(principal, forceRefresh = refresh)
   }

   override fun getLicenseStatus(principal: Mono<Authentication>?): Mono<LicenseWithUsage> {
      return getLicenseStatus(principal, forceRefresh = false)
   }

   private fun getLicenseStatus(principal: Mono<Authentication>?, forceRefresh: Boolean): Mono<LicenseWithUsage> {
      // We need a principal.
      // However, other whitelabel solutions may have different licensing options
      // that allow fetching / updating license without requiring being logged in.
      // Therefore, we can't use Spring's auth manager on this endpoint, so
      // we validate the presence of a principal manually
      if (principal == null) {
         return Mono.error(NotAuthorizedException("You need to be logged in to access this API"))
      }

      val downloadLicense = when {
         // If we don't have an actual license, download one.
         // We can't do this pre-emptively, as we need a users credentials in order to fetch the
         // license --
         // Licenses are attached to Organisations, so we use the users credentials to authenticate
         // to the license manager
         // (which - assuming they're logged in at auth.orbitalhq.com - will contain their
         // organisation)
         licenseManager.license.isFallbackLicense -> {
            logger.info { "No active license - requesting one" }
            true
         }

         forceRefresh -> {
            logger.info { "Refreshing license by downloading from license server" }
            true
         }

         else -> false
      }

      return if (downloadLicense) {
         principal.flatMap { auth ->
            require(auth is JwtAuthenticationToken) { "Cannot fetch license with auth token of type ${auth::class.simpleName} - expected a JwtToken" }
            val bearerToken = "Bearer ${auth.token.tokenValue}"
            licenseManager.downloadLicense(bearerToken)
         }
      } else {
         return super.getLicenseStatus(principal)
      }
   }
}

abstract class BaseLicenseStatusService(private val licenseManager: LicenseManager) {
   @GetMapping("/api/license/status")
   open fun getLicenseStatus(@AuthenticationPrincipal principal: Mono<Authentication>? = null): Mono<LicenseWithUsage> {
      // stubbed for now
      val usage = licenseManager.license.quotas.map {
         QuotaUsageState(it, 0)
      }
      return Mono.just(
         LicenseWithUsage(
            licenseManager.license,
            usage
         )
      )
   }
}

/**
 * Contains both high-level license information, as well as usage metrics
 * against licesned quotas
 */
data class LicenseWithUsage(
   @JsonIgnore
   val license: License,
   val usage: List<QuotaUsageState>
) {
   val licenseSummary: LicenseSummary = license.summary
}

data class QuotaUsageState(
   val quota: UsageQuota,
   val usage: Long
) {
   val health: QuotaHealth = QuotaHealth.calculateFor(usage, quota.limit)
}

enum class QuotaHealth {
   HEALTHY,
   WARNING,
   EXCEEDED;

   companion object {
      fun calculateFor(consumedUsage: Long, quota: Long): QuotaHealth {
         return when {
            // avoid div/0 errors
            quota == 0L && consumedUsage == 0L -> WARNING
            consumedUsage > quota -> EXCEEDED
            consumedUsage / quota.toDouble() < 0.75 -> HEALTHY
            else -> WARNING
         }
      }
   }
}

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class NotAuthorizedException(message: String = "This operation is not permitted") : RuntimeException(message)
