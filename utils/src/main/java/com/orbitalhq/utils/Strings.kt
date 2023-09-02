package com.orbitalhq.utils

import com.google.common.hash.Hashing
import org.apache.commons.lang3.StringUtils

fun String.withoutWhitespace(): String {
   return this
      .lines()
      .map { it.trim().replace(" ", "") }
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

fun String.abbreviate(length: Int = 50): String {
   return StringUtils.abbreviate(this, length)
}

private val hashCharset = java.nio.charset.Charset.defaultCharset()
fun List<String>.shaHash(): String {
   val hasher = Hashing.sha256().newHasher()
   this.forEach { hasher.putString(it, hashCharset) }
   return hasher
      .hash()
      .toString()
}
