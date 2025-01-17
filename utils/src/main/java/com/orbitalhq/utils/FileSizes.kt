package com.orbitalhq.utils

import org.apache.commons.io.FileUtils
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
   get() = FileUtils.byteCountToDisplaySize(this)
