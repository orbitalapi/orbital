package io.vyne.schemaStore.eureka

object EurekaMetadata {
   const val VYNE_SCHEMA_URL = "vyne.schema.url"
   const val VYNE_SOURCE_PREFIX = "vyne.sources."
   const val COLON = ":"
   const val FWD_SL = "/"
   const val COLON_REPLACER = "___"
   const val FWD_SL_REPLACER = "---"

   fun escapeForXML(input: String): String {
      return input.replace(COLON, COLON_REPLACER).replace(FWD_SL, FWD_SL_REPLACER)
   }

   fun fromXML(input: String): String {
      return input.replace(COLON_REPLACER, COLON).replace(FWD_SL_REPLACER, FWD_SL)
   }

   fun isVyneMetadata(metadataKey: String) = metadataKey.startsWith("vyne")
}

