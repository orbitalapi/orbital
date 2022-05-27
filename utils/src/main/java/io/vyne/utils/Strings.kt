package io.vyne.utils

fun String.withoutWhitespace(): String {
   return this
      .lines()
      .map { it.trim().replace(" ","") }
      .filter { it.isNotEmpty() }
      .joinToString("")
}

fun String.withoutEmptyLines(): String {
   return this.trim()
      .lines()
      .filterNot { it.isBlank() }
      .joinToString("\n") { it.trimEnd() }
}

fun String.substitute(inputs: Map<String, Any>): String {
   return inputs.entries.fold(this) { acc, entry ->
      val (key, value) = entry
      acc.replace("{$key}", value.toString())
   }
}
