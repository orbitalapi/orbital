package io.vyne.schema.api

enum class VyneSchemaInteractionMethod {
   // Non-standard casing as these are user-supplied
   // config values.
   // However, lets try to be consistent
   RSocket,
   Http,
   // Used in testing, not for production use.
   Local;

   companion object {
      fun tryParse(value: String): VyneSchemaInteractionMethod? {
         return try {
            valueOf(value)
         } catch(e: Exception) { null }
      }
   }
}
