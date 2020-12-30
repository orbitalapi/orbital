package io.vyne.schemaServer.local

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.nio.file.Path

class LocalFileService(private val home: Path, private val permittedExtensions: List<String>) {
   companion object {
      val DEFAULT_EXTENSIONS = listOf("taxi")
   }

   fun writeContents(path: Path, contents: ByteArray) {
      validatePath(path)
      path.toFile().writeBytes(contents)
   }

   private fun validatePath(path: Path): Path {
      val extension = path.toFile().extension
      if (!permittedExtensions.contains(extension)) {
         throw IllegalPathException("Extension $extension is not permitted")
      }
      val resolved = home.resolve(path.normalize())
      if (!resolved.startsWith(home)) {
         throw IllegalPathException("Path ${path.toAbsolutePath()} is outside of root path")
      }
      return resolved
   }

   fun writeContents(path: Path, contents: String) {
      return writeContents(path, contents.toByteArray())
   }

}

@ResponseStatus(HttpStatus.BAD_REQUEST)
class IllegalPathException(message: String) : RuntimeException(message)
