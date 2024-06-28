package com.orbitalhq.licensing

import mu.KotlinLogging
import reactor.core.publisher.Sinks

open class LicenseManager(initialLicense: License) {

   var license: License = initialLicense
      private set;

   private val licenseUpdatedSink = Sinks.many().multicast().directAllOrNothing<LicenseWithUsage>()
   val licenseUpdated = licenseUpdatedSink.asFlux()

   companion object {
      private val logger = KotlinLogging.logger {}
   }

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

   /**
    * Updates the license without further verification
    */
   fun submitValidatedLicense(license: License): LicenseWithUsage {
      logger.info { "License updated: New license - $license" }
      this.license = license
      licenseUpdatedSink.tryEmitNext(getLicenseStatus())
      return getLicenseStatus()
   }
}
