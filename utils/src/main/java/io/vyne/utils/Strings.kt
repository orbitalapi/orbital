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
