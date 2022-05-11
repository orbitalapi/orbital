package io.vyne.schemaServer.core.editor

import io.vyne.schemaServer.core.file.FileSystemSchemaRepository
import io.vyne.spring.http.BadRequestException
import mu.KotlinLogging
import java.nio.file.Path
import java.nio.file.Paths

object EditingDisabledRepository : io.vyne.schemaServer.core.editor.ApiEditorRepository {
   private val logger = KotlinLogging.logger {}
   override val fileRepository: FileSystemSchemaRepository
      get() {
         io.vyne.schemaServer.core.editor.EditingDisabledRepository.logger.warn { "Received attempt to make edits but no ApiEditorRepository has been configured." }
         throw BadRequestException("Schema editing is not enabled on this service")
      }
}

interface ApiEditorRepository {
   val fileRepository: FileSystemSchemaRepository
}


/**
 * A wrapper around another existing FileSystemSchemaRepository, which becomes
 * the repository where changes are written to, if received from the API
 */
class DefaultApiEditorRepository(override val fileRepository: FileSystemSchemaRepository) : io.vyne.schemaServer.core.editor.ApiEditorRepository {

   companion object {
      private val logger = KotlinLogging.logger {}
      fun forPath(path: String): io.vyne.schemaServer.core.editor.DefaultApiEditorRepository {
         return io.vyne.schemaServer.core.editor.DefaultApiEditorRepository.Companion.forPath(Paths.get(path))
      }

      fun forPath(path: Path): io.vyne.schemaServer.core.editor.DefaultApiEditorRepository {
         return io.vyne.schemaServer.core.editor.DefaultApiEditorRepository(
            FileSystemSchemaRepository.forPath(path)
         )
      }
   }
}
