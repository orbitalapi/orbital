package com.orbitalhq.utils

import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths


fun String.toPath(): Path {
   return File(asResource().file).toPath()
}

fun String.asResource(): URL = Thread.currentThread().contextClassLoader.getResource(this)

fun resolvePossiblyAbsolutePath(pathString: String, relativeTo: Path): Path {
   return try {
      val path = Paths.get(URI.create(pathString))
      if (!path.isAbsolute) {
         relativeTo.resolve(pathString)
      } else path
   } catch (e:Exception) {
      relativeTo.resolve(pathString)
   }
}
