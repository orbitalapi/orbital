package io.vyne.licensing

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

data class License(
   val licensee: String,
   @JsonFormat(shape = JsonFormat.Shape.STRING)
   val expiresOn: Instant,
   @JsonFormat(shape = JsonFormat.Shape.STRING)
   val timestamp: Instant,
   val edition: LicensedEdition,
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
      fun unlicensed(expiresOn: Instant): License {
         return License(
            "Unlicensed",
            expiresOn,
            Instant.now(),
            LicensedEdition.ENTERPRISE,
            true
         )
      }
      private val dateFormat = DateTimeFormatter
         .ofLocalizedDateTime(FormatStyle.FULL)
         .withLocale(Locale.getDefault())
         .withZone(ZoneId.systemDefault())

   }

   override fun toString(): String {
      return "Licensed to ${this.licensee} with ${this.edition.name.toLowerCase().capitalize()} edition.  Expires on ${dateFormat.format(this.expiresOn)}."
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
}

enum class LicensedEdition(val enabledFeatures: List<LicensedFeature>, val limits: List<Limitation>) {
   STARTER(enabledFeatures = emptyList(), limits = emptyList()),
   PLATFORM(enabledFeatures = emptyList(), limits = emptyList()),
   ENTERPRISE(enabledFeatures = emptyList(), limits = emptyList()),
}

data class Limitation(
   val name: String,
   val limit: Int
)

enum class LicensedFeature {

}
