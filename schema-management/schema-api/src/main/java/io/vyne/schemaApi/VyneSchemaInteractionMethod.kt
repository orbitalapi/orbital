package io.vyne.schemaApi

enum class VyneSchemaInteractionMethod {
   RSOCKET,
   HTTP;

   companion object {
      fun tryParse(value: String): VyneSchemaInteractionMethod? {
         return try {
            valueOf(value)
         } catch(e: Exception) { null }
      }
   }
}
