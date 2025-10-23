package com.orbitalhq.test.utils

/**
 * Asserts that this string equals the expected string, ignoring all whitespace differences.
 * This includes spaces, tabs, newlines, and carriage returns.
 *
 * Useful for comparing formatted output like XML, JSON, or HTML where whitespace is not semantically significant.
 *
 * @param expected the expected string value
 * @throws AssertionError if the strings are not equal after normalizing whitespace
 */
infix fun String.shouldEqualIgnoringWhitespace(expected: String) {
   val normalizedActual = this.replace("\\s+".toRegex(), "")
   val normalizedExpected = expected.replace("\\s+".toRegex(), "")

   if (normalizedActual != normalizedExpected) {
      // Provide helpful error message showing both normalized and original
      val message = buildString {
         appendLine("Strings are not equal (ignoring whitespace)")
         appendLine()
         appendLine("Expected (normalized):")
         appendLine(normalizedExpected)
         appendLine()
         appendLine("Actual (normalized):")
         appendLine(normalizedActual)
         appendLine()
         appendLine("Expected (original):")
         appendLine(expected)
         appendLine()
         appendLine("Actual (original):")
         appendLine(this@shouldEqualIgnoringWhitespace)

         // Try to find where they differ
         val diffIndex = normalizedActual.zip(normalizedExpected)
            .indexOfFirst { (a, b) -> a != b }

         if (diffIndex >= 0) {
            appendLine()
            appendLine("First difference at position $diffIndex:")
            val contextStart = maxOf(0, diffIndex - 20)
            val contextEnd = minOf(normalizedActual.length, diffIndex + 20)
            appendLine("Expected: ...${normalizedExpected.substring(contextStart, minOf(normalizedExpected.length, contextEnd))}...")
            appendLine("Actual:   ...${normalizedActual.substring(contextStart, contextEnd)}...")
         }
      }
      throw AssertionError(message)
   }
}

/**
 * Asserts that this string does not equal the expected string, ignoring all whitespace differences.
 */
infix fun String.shouldNotEqualIgnoringWhitespace(expected: String) {
   val normalizedActual = this.replace("\\s+".toRegex(), "")
   val normalizedExpected = expected.replace("\\s+".toRegex(), "")

   if (normalizedActual == normalizedExpected) {
      throw AssertionError("Strings should not be equal (ignoring whitespace) but both normalize to:\n$normalizedActual")
   }
}
