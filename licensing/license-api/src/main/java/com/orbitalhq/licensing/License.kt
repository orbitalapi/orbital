package com.orbitalhq.licensing

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

data class License(
   val licensee: String,
   @JsonFormat(shape = JsonFormat.Shape.STRING)
   val expiresOn: Instant,
   @JsonFormat(shape = JsonFormat.Shape.STRING)
   val timestamp: Instant,
   val edition: LicensedEdition,
   val quotas: List<UsageQuota>,
   /**
    * Indciates that this license has been used becuase
    * one either wasn't provided, or wasn't valid (ie., was expired,
    * or not signed correctly)
    */
   @JsonIgnore
   val isFallbackLicense: Boolean = false,
   val signature: String? = null
) {
   companion object {
      fun unlicensed(expiresOn: Instant, licensee: String): License {
         return License(
            licensee,
            expiresOn,
            Instant.now(),
            LicensedEdition.STARTER,
            emptyList(),
            true
         )
      }

      private val dateFormat = DateTimeFormatter
         .ofLocalizedDateTime(FormatStyle.FULL)
         .withLocale(Locale.UK)
         .withZone(ZoneId.of("UTC"))

   }

   override fun toString(): String {
      return "Licensed to ${this.licensee} with ${
         this.edition.name.toLowerCase().capitalize()
      } edition.  Expires on ${dateFormat.format(this.expiresOn)}."
   }

   fun unsigned(): License = this.copy(signature = null)
   fun signed(signature: String) = this.copy(signature = signature)

   /**
    * Returns this license as a string to either sign or verify
    */
   fun verificationClaim(): ByteArray {
      require(this.signature == null) { "You should not verify an already signed license, as that mutates the claim.  Call .unlicensed()" }
      return Signing.objectMapper.writeValueAsBytes(this)
   }

   fun quota(capability: MeteredCapability):UsageQuota {
      return this.quotas.firstOrNull { it.capability == capability }
         ?: UsageQuota(capability, 0)
   }
}

/**
 * We group our capabilities into "Editions" - basically a collection
 * of features that are enabled for the license
 */
enum class LicensedEdition(val enabledFeatures: List<LicensedFeature>) {
   STARTER(enabledFeatures = emptyList()),
   PLATFORM(enabledFeatures = emptyList()),
   ENTERPRISE(enabledFeatures = emptyList()),
}

data class UsageQuota(
   val capability: MeteredCapability,
   val limit: Int
)

enum class MeteredCapability {
   User,
   Endpoint,
   Invocation
}

enum class LicensedFeature {

}
