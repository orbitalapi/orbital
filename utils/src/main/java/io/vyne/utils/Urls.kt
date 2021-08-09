package io.vyne.utils

import java.net.URL
import java.net.URLConnection
import java.nio.charset.Charset


fun URL.readByteArray(config: URLConnection.() -> Unit = {}): ByteArray =
   openConnection()
      .apply(config)
      .getInputStream()
      .use { it.readBytes() }

fun URL.readString(
   charset: Charset = Charsets.UTF_8,
   config: URLConnection.() -> Unit = {}
): String = readByteArray(config).toString(charset)
