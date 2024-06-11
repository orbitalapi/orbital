package com.orbitalhq.licensing

import arrow.core.getOrElse
import mu.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class LicenseService(
   private var license: License,
   private val validator: LicenseValidator
) {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   @GetMapping("/api/license/status")
   fun getLicenseStatus(): LicenseWithUsage {
      // stubbed for now
      val usage = this.license.quotas.map {
         QuotaUsageState(it, 0)
      }
      return LicenseWithUsage(
         this.license,
         usage
      )
   }

   @PostMapping("/api/license")
   fun submitLicense(@RequestBody licenseJson: String): LicenseWithUsage {
      val validationResult = validator.readAndValidateLicense(licenseJson)
      val validatedLicense = validationResult.getOrElse { exception -> throw exception }
      return submitValidatedLicense(validatedLicense)
   }

   /**
    * Updates the license without further verification
    */
   fun submitValidatedLicense(license: License): LicenseWithUsage {
      logger.info { "License updated: New license - $license" }
      this.license = license
      return getLicenseStatus()
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
   val usage: Int
) {
   val health: QuotaHealth = QuotaHealth.calculateFor(usage, quota.limit)
}

enum class QuotaHealth {
   HEALTHY,
   WARNING,
   EXCEEDED;

   companion object {
      fun calculateFor(consumedUsage: Int, quota: Int): QuotaHealth {
         return when {
            // avoid div/0 errors
            quota == 0 && consumedUsage == 0 -> WARNING
            consumedUsage > quota -> EXCEEDED
            consumedUsage / quota.toDouble() < 0.75 -> HEALTHY
            else -> WARNING
         }
      }
   }
}
