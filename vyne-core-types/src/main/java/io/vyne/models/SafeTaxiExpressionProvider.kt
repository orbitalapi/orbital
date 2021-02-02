package io.vyne.models

import io.vyne.utils.log
import lang.taxi.types.TaxiStatementGenerator

/**
 * Returns the taxi statement, catching any errors
 */
fun TaxiStatementGenerator.safeTaxi(): String {
   return try {
      this.asTaxi()
   } catch (e: Exception) {
      log().warn("Failed to capture taxi from taxiStatement - exception: ${e::class.simpleName} - ${e.message}")
      "Unknown taxi"
   }
}
