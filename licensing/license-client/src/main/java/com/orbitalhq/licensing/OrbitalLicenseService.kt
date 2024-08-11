package com.orbitalhq.licensing

import arrow.core.getOrElse
import com.orbitalhq.security.VynePrivileges
import com.orbitalhq.spring.config.OrbitalOnly
import mu.KotlinLogging
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Sinks

@RestController
@OrbitalOnly
class OrbitalLicenseService(
   private val licenseManager: LicenseManager,
   private val validator: LicenseValidator
) : BaseLicenseStatusService(licenseManager) {

   private val licenseUpdatedSink = Sinks.many().multicast().directAllOrNothing<LicenseWithUsage>()
   val licenseUpdated = licenseUpdatedSink.asFlux()

   @PostMapping("/api/license")
   @PreAuthorize("hasAuthority('${VynePrivileges.ModifyLicense}')")
   suspend fun submitLicense(@RequestBody licenseJson: String): LicenseWithUsage {
      val validationResult = validator.readAndValidateLicense(licenseJson)
      val validatedLicense = validationResult.getOrElse { exception -> throw exception }
      return licenseManager.submitValidatedLicense(validatedLicense)
   }
}

abstract class BaseLicenseStatusService(private val licenseManager: LicenseManager) {
   @GetMapping("/api/license/status")
   fun getLicenseStatus(): LicenseWithUsage {
      // stubbed for now
      val usage = licenseManager.license.quotas.map {
         QuotaUsageState(it, 0)
      }
      return LicenseWithUsage(
         licenseManager.license,
         usage
      )
   }
}

/**
 * Contains both high-level license information, as well as usage metrics
 * against licesned quotas
 */
data class LicenseWithUsage(
   val license: License,
   val usage: List<QuotaUsageState>
)

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
