package com.orbitalhq.utils

import java.io.File
import kotlin.math.log2
import kotlin.math.pow

/**
 * @author aminography
 * from: https://stackoverflow.com/a/39002685/59015
 */

val File.formatSize: String
   get() = length().formatAsFileSize

val Int.formatAsFileSize: String
   get() = toLong().formatAsFileSize

val Long.formatAsFileSize: String
   get() = log2(coerceAtLeast(1).toDouble()).toInt().div(10).let {
      val precision = when (it) {
         0 -> 0; 1 -> 1; else -> 2
      }
      val prefix = arrayOf("", "K", "M", "G", "T", "P", "E", "Z", "Y")
      String.format("%.${precision}f ${prefix[it]}B", toDouble() / 2.0.pow(it * 10.0))
   }
