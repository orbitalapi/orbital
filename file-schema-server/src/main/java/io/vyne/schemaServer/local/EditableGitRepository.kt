package io.vyne.schemaServer.local

import io.vyne.schemaServer.git.Author
import io.vyne.schemaServer.git.GitRepo
import java.nio.file.Path

class EditableGitRepository(private val repo: GitRepo, private val fileService: LocalFileService) {
   constructor(repo: GitRepo, allowedExtensions: List<String> = LocalFileService.DEFAULT_EXTENSIONS) : this(repo, LocalFileService(repo.workingDir, allowedExtensions))

   fun updateFile(path: Path, contents: ByteArray, author: Author, commitMessage: String, push: Boolean = false) {
      fileService.writeContents(path, contents)
      repo.commitFile(path, author, commitMessage)

      if (push) {
         repo.push()
      }
   }
}
