package io.vyne.schemaServer.local

import io.vyne.schemaServer.git.Author
import io.vyne.schemaServer.git.CommitResult
import io.vyne.schemaServer.git.GitRepo
import io.vyne.schemaServer.git.OperationResult
import java.nio.file.Path

class EditableGitRepository(private val repo: GitRepo, private val fileService: LocalFileService) {
   val name = repo.name

   constructor(repo: GitRepo, allowedExtensions: List<String> = LocalFileService.DEFAULT_EXTENSIONS) : this(
      repo,
      LocalFileService(repo.workingDir, allowedExtensions)
   )

   fun updateFile(
      path: Path,
      contents: ByteArray,
      author: Author,
      commitMessage: String,
      push: Boolean = false
   ): UpdateFileResult {
      val writtenPath = fileService.writeContents(path, contents)
      val commitResult = repo.commitFile(writtenPath, author, commitMessage)

      val pushResult = if (push) {
         repo.push()
      } else {
         OperationResult.NOT_ATTEMPTED
      }

      return UpdateFileResult(commitResult, pushResult)
   }
}

data class UpdateFileResult(
   val commitResult: CommitResult,
   val pushResult: OperationResult
)
