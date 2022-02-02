package io.vyne.schemaServerCore.editor

import io.vyne.schemaServerCore.file.FileSystemSchemaRepository
import io.vyne.spring.http.BadRequestException
import mu.KotlinLogging
import java.nio.file.Path
import java.nio.file.Paths

object EditingDisabledRepository : ApiEditorRepository {
   private val logger = KotlinLogging.logger {}
   override val fileRepository: FileSystemSchemaRepository
      get() {
         logger.warn { "Received attempt to make edits but no ApiEditorRepository has been configured." }
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
class DefaultApiEditorRepository(override val fileRepository: FileSystemSchemaRepository) : ApiEditorRepository {

   companion object {
      private val logger = KotlinLogging.logger {}
      fun forPath(path: String): DefaultApiEditorRepository {
         return forPath(Paths.get(path))
      }

      fun forPath(path: Path): DefaultApiEditorRepository {
         return DefaultApiEditorRepository(
            FileSystemSchemaRepository.forPath(path)
         )
      }
   }
}
