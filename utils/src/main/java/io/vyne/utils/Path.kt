package io.vyne.utils

import java.io.File
import java.net.URL
import java.nio.file.Path


fun String.toPath(): Path {
   return File(asResource().file).toPath()
}

fun String.asResource(): URL = Thread.currentThread().contextClassLoader.getResource(this)
