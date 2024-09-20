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
   val plan: String,
   // The set of features a user has enabled.
   // By default, it's the set contained in their edition,
   // but they can choose to bundle additional extras
   val features: List<LicensedFeature>,
   val quotas: List<UsageQuota>,
   /**
    * Indicates that this license has been used because
    * one either wasn't provided, or wasn't valid (ie., was expired,
    * or not signed correctly
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
            "Unlicensed",
            emptyList(),
            emptyList(),
            true
         )
      }

      private val dateFormat = DateTimeFormatter
         .ofLocalizedDateTime(FormatStyle.FULL)
         .withLocale(Locale.UK)
         .withZone(ZoneId.of("UTC"))

   }

   @JsonIgnore
   val summary = LicenseSummary(
      licensee, expiresOn, timestamp, plan
   )

   override fun toString(): String {
      return "Licensed to ${this.licensee} with ${
         this.plan
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

   fun quota(capability: MeteredCapability): UsageQuota {
      return this.quotas.firstOrNull { it.capability == capability }
         ?: UsageQuota(capability, 0)
   }
}

/**
 * Intended for sending to the UI.
 * Excludes the usage quotas, which are sent with LicenseUsage
 */
data class LicenseSummary(
   val licensee: String,
   @JsonFormat(shape = JsonFormat.Shape.STRING)
   val expiresOn: Instant,
   @JsonFormat(shape = JsonFormat.Shape.STRING)
   val timestamp: Instant,
   /**
    * A name of the plan, for display purposes.
    */
   val plan: String,
)

data class UsageQuota(
   val capability: MeteredCapability,
   val limit: Long
)

enum class MeteredCapability {
   User,
   Endpoint,
   Invocation
}

/**
 * A Feature is a platform capability or permission that can be enabled or disabled.
 */
enum class LicensedFeature {

   /**
    * The customer can use their own IDP, instead of auth.orbitalhq.com
    */
   AllowOwnIdp,

   /**
    * The customer is entitled to author data policies
    */
   DataPolicies
}
