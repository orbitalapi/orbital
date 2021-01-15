package io.vyne.schemaServer.local

import io.vyne.utils.log
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.nio.file.Path

class LocalFileService(private val home: Path, private val permittedExtensions: List<String>) {
   companion object {
      val DEFAULT_EXTENSIONS = listOf("taxi")
   }

   /**
    * Writes contents to a path, relative to the home path.
    * Validates that the extension of the file is permitted, and that the location of the file is within the home path.
    *
    * Returns the full path of the written file.
    */
   fun writeContents(path: Path, contents: ByteArray):Path {
      val resolvedPath = validatePath(path)

      val outputFile = resolvedPath.toFile()
      outputFile.parentFile.mkdirs()
      outputFile.writeBytes(contents)
      log().info("Updated contents of file $resolvedPath")

      return resolvedPath
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

   fun writeContents(path: Path, contents: String):Path {
      return writeContents(path, contents.toByteArray())
   }

}

@ResponseStatus(HttpStatus.BAD_REQUEST)
class IllegalPathException(message: String) : RuntimeException(message)
