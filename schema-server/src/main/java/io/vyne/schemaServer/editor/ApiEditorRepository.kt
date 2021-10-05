package io.vyne.schemaServer.editor

import io.vyne.schemaServer.file.FileSystemSchemaRepository
import mu.KotlinLogging
import java.nio.file.Path
import java.nio.file.Paths

/**
 * A wrapper around another existing FileSystemSchemaRepository, which becomes
 * the repository where changes are written to, if received from the API
 */
class ApiEditorRepository(val fileRepository: FileSystemSchemaRepository) {

   companion object {
      private val logger = KotlinLogging.logger {}
      fun forPath(path:String): ApiEditorRepository {
         return forPath(Paths.get(path))
      }
      fun forPath(path: Path):ApiEditorRepository {
         return ApiEditorRepository(
            FileSystemSchemaRepository.forPath(path)
         )
      }
   }
}
